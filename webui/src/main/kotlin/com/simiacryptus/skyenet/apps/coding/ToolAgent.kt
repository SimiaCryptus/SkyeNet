package com.simiacryptus.skyenet.apps.coding

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.describe.AbbrevWhitelistYamlDescriber
import com.simiacryptus.jopenai.describe.TypeDescriber
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.core.actors.CodingActor
import com.simiacryptus.skyenet.core.actors.CodingActor.CodeResult
import com.simiacryptus.skyenet.core.actors.CodingActor.Companion.indent
import com.simiacryptus.skyenet.core.actors.CodingActor.Companion.sortCode
import com.simiacryptus.skyenet.core.actors.ParsedActor
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.platform.*
import com.simiacryptus.skyenet.interpreter.Interpreter
import com.simiacryptus.skyenet.kotlin.KotlinInterpreter
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.servlet.ToolServlet
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import com.simiacryptus.skyenet.webui.util.OpenAPI
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Response
import org.eclipse.jetty.webapp.WebAppClassLoader
import org.openapitools.codegen.OpenAPIGenerator
import org.openapitools.codegen.SpecValidationException
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.reflect.KClass

abstract class ToolAgent<T : Interpreter>(
    api: API,
    dataStorage: StorageInterface,
    session: Session,
    user: User?,
    ui: ApplicationInterface,
    interpreter: KClass<T>,
    symbols: Map<String, Any>,
    temperature: Double = 0.1,
    details: String? = null,
    model: ChatModels,
    mainTask: SessionTask = ui.newTask(),
    actorMap: Map<ActorTypes, CodingActor> = mapOf(
        ActorTypes.CodingActor to CodingActor(
            interpreter,
            symbols = symbols,
            temperature = temperature,
            details = details,
            model = model
        )
    ),
) : CodingAgent<T>(
    api,
    dataStorage,
    session,
    user,
    ui,
    interpreter,
    symbols,
    temperature,
    details,
    model,
    mainTask,
    actorMap
) {
    override fun displayFeedback(task: SessionTask, request: CodingActor.CodeRequest, response: CodeResult) {
        val formText = StringBuilder()
        var formHandle: StringBuilder? = null
        formHandle = task.add(
            """
      |<div style="display: flex;flex-direction: column;">
      |${super.playButton(task, request, response, formText) { formHandle!! }}
      |${super.regenButton(task, request, formText) { formHandle!! }}
      |${createToolButton(task, request, response, formText) { formHandle!! }}
      |</div>  
      |${super.reviseMsg(task, request, response, formText) { formHandle!! }}
      """.trimMargin(), className = "reply-message"
        )
        formText.append(formHandle.toString())
        formHandle.toString()
        task.complete()
    }

    private fun createToolButton(
        task: SessionTask,
        request: CodingActor.CodeRequest,
        response: CodeResult,
        formText: StringBuilder,
        formHandle: () -> StringBuilder
    ) = ui.hrefLink("\uD83D\uDCE4", "href-link regen-button") {
        val task = ui.newTask()
        responseAction(task, "Exporting...", formHandle(), formText) {
            displayCodeFeedback(
                task, schemaActor(), request.copy(
                    messages = listOf(
                        response.code to ApiModel.Role.assistant,
                        "From the given code prototype, identify input out output data structures and generate Kotlin data classes to define this schema" to ApiModel.Role.user
                    )
                )
            ) { schemaCode ->
                displayCodeFeedback(
                    task, servletActor(), request.copy(
                        messages = listOf(
                            response.code to ApiModel.Role.assistant,
                            "Reprocess this code prototype into a servlet using the given data schema. " +
                                    "The last line should instantiate the new servlet class and return it via the returnBuffer collection." to ApiModel.Role.user
                        ),
                        codePrefix = schemaCode
                    )
                ) { servletHandler ->
                    val servletImpl = (schemaCode + "\n\n" + servletHandler).sortCode()
                    val toolsPrefix = "/tools"
                    var openAPI = openAPIParsedActor().getParser(api).apply(servletImpl).let { openApi ->
                        openApi.copy(paths = openApi.paths?.mapKeys { toolsPrefix + it.key.removePrefix(toolsPrefix) })
                    }
                    task.add(renderMarkdown("```json\n${JsonUtil.toJson(openAPI)/*.indent("  ")*/}\n```", ui = ui))
                    for (i in 0..5) {
                        try {
                            OpenAPIGenerator.main(
                                arrayOf(
                                    "generate",
                                    "-i",
                                    File.createTempFile("openapi", ".json").apply {
                                        writeText(JsonUtil.toJson(openAPI))
                                        deleteOnExit()
                                    }.absolutePath,
                                    "-g",
                                    "html2",
                                    "-o",
                                    File(
                                        dataStorage.getSessionDir(user, session),
                                        "openapi/html2"
                                    ).apply { mkdirs() }.absolutePath,
                                )
                            )
                            task.add("Validated OpenAPI Descriptor - <a href='fileIndex/$session/openapi/html2/index.html'>Documentation Saved</a>")
                            break
                        } catch (e: SpecValidationException) {
                            val error = """
              |${e.message}
              |${e.errors.joinToString("\n") { "ERROR:" + it.toString() }}
              |${e.warnings.joinToString("\n") { "WARN:" + it.toString() }}
            """.trimIndent()
                            task.hideable(ui, renderMarkdown("```\n${error/*.indent("  ")*/}\n```", ui = ui))
                            openAPI = openAPIParsedActor().answer(
                                listOf(
                                    servletImpl,
                                    JsonUtil.toJson(openAPI),
                                    error
                                ), api
                            ).obj.let { openApi ->
                                val paths = HashMap(openApi.paths)
                                openApi.copy(paths = paths.mapKeys { toolsPrefix + it.key.removePrefix(toolsPrefix) })
                            }
                            task.hideable(
                                ui,
                                renderMarkdown("```json\n${JsonUtil.toJson(openAPI)/*.indent("  ")*/}\n```", ui = ui)
                            )
                        }
                    }
                    if (ApplicationServices.authorizationManager.isAuthorized(
                            ToolAgent.javaClass,
                            user,
                            AuthorizationInterface.OperationType.Admin
                        )
                    ) {
                        ToolServlet.addTool(
                            ToolServlet.Tool(
                                path = openAPI.paths?.entries?.first()?.key?.removePrefix(toolsPrefix) ?: "unknown",
                                openApiDescription = openAPI,
                                interpreterString = getInterpreterString(),
                                servletCode = servletImpl
                            )
                        )
                    }
                    buildTestPage(openAPI, servletImpl, task)
                }
            }
        }
    }

    private fun openAPIParsedActor() = object : ParsedActor<OpenAPI>(
//    parserClass = OpenApiParser::class.java,
        resultClass = OpenAPI::class.java,
        model = model,
        prompt = "You are a code documentation assistant. You will create the OpenAPI definition for a servlet handler written in kotlin",
        parsingModel = model,
    ) {
        override val describer: TypeDescriber
            get() = object : AbbrevWhitelistYamlDescriber(
                //"com.simiacryptus", "com.github.simiacryptus"
            ) {
                override val includeMethods: Boolean get() = false
            }
    }

    private fun servletActor() = object : CodingActor(
        interpreterClass = actor.interpreterClass,
        symbols = actor.symbols + mapOf(
            "returnBuffer" to ServletBuffer(),
            "json" to JsonUtil,
            "req" to Request(null, null),
            "resp" to Response(null, null),
        ),
        describer = object : AbbrevWhitelistYamlDescriber(
            "com.simiacryptus",
            "com.github.simiacryptus"
        ) {
            override fun describe(
                rawType: Class<in Nothing>,
                stackMax: Int,
                describedTypes: MutableSet<String>
            ): String = when (rawType) {
                Request::class.java -> describe(HttpServletRequest::class.java)
                Response::class.java -> describe(HttpServletResponse::class.java)
                else -> super.describe(rawType, stackMax, describedTypes)
            }
        },
        details = actor.details,
        model = actor.model,
        fallbackModel = actor.fallbackModel,
        temperature = actor.temperature,
        runtimeSymbols = actor.runtimeSymbols
    ) {
        override val prompt: String
            get() = super.prompt
    }

    private fun schemaActor() = object : CodingActor(
        interpreterClass = actor.interpreterClass,
        symbols = mapOf(),
        details = actor.details,
        model = actor.model,
        fallbackModel = actor.fallbackModel,
        temperature = actor.temperature,
        runtimeSymbols = actor.runtimeSymbols
    ) {
        override val prompt: String
            get() = super.prompt
    }


    private fun displayCodeFeedback(
        task: SessionTask,
        actor: CodingActor,
        request: CodingActor.CodeRequest,
        response: CodeResult = execWrap { actor.answer(request, api = api) },
        onComplete: (String) -> Unit
    ) {
        task.hideable(ui, renderMarkdown("```kotlin\n${/*escapeHtml4*/(response.code)/*.indent("  ")*/}\n```", ui = ui))
        val formText = StringBuilder()
        var formHandle: StringBuilder? = null
        formHandle = task.add(
            """
      |<div style="display: flex;flex-direction: column;">
      |${
                if (!super.canPlay) "" else
                    super.ui.hrefLink("\uD83D\uDC4D", "href-link play-button") {
                        super.responseAction(task, "Accepted...", formHandle!!, formText) {
                            onComplete(response.code)
                        }
                    }
            }
      |${
                super.ui.hrefLink("♻", "href-link regen-button") {
                    super.responseAction(task, "Regenerating...", formHandle!!, formText) {
                        //val task = super.ui.newTask()
                        val codeRequest =
                            request.copy(messages = request.messages.dropLastWhile { it.second == ApiModel.Role.assistant })
                        try {
                            val lastUserMessage =
                                codeRequest.messages.last { it.second == ApiModel.Role.user }.first.trim()
                            val codeResponse: CodeResult = if (lastUserMessage.startsWith("```")) {
                                actor.CodeResultImpl(
                                    messages = actor.chatMessages(codeRequest),
                                    input = codeRequest,
                                    api = super.api as OpenAIClient,
                                    givenCode = lastUserMessage.removePrefix("```").removeSuffix("```")
                                )
                            } else {
                                actor.answer(codeRequest, api = super.api)
                            }
                            super.displayCode(task, codeResponse)
                            displayCodeFeedback(
                                task,
                                actor,
                                super.append(codeRequest, codeResponse),
                                codeResponse,
                                onComplete
                            )
                        } catch (e: Throwable) {
                            log.warn("Error", e)
                            val error = task.error(super.ui, e)
                            var regenButton: StringBuilder? = null
                            regenButton = task.complete(super.ui.hrefLink("♻", "href-link regen-button") {
                                regenButton?.clear()
                                val header = task.header("Regenerating...")
                                super.displayCode(task, codeRequest)
                                header?.clear()
                                error?.clear()
                                task.complete()
                            })
                        }
                    }
                }
            }
      |</div>  
      |${
                super.ui.textInput { feedback ->
                    super.responseAction(task, "Revising...", formHandle!!, formText) {
                        //val task = super.ui.newTask()
                        try {
                            task.echo(renderMarkdown(feedback, ui = ui))
                            val codeRequest = CodingActor.CodeRequest(
                                messages = request.messages +
                                        listOf(
                                            response.code to ApiModel.Role.assistant,
                                            feedback to ApiModel.Role.user,
                                        ).filter { it.first.isNotBlank() }.map { it.first to it.second }
                            )
                            try {
                                val lastUserMessage =
                                    codeRequest.messages.last { it.second == ApiModel.Role.user }.first.trim()
                                val codeResponse: CodeResult = if (lastUserMessage.startsWith("```")) {
                                    actor.CodeResultImpl(
                                        messages = actor.chatMessages(codeRequest),
                                        input = codeRequest,
                                        api = super.api as OpenAIClient,
                                        givenCode = lastUserMessage.removePrefix("```").removeSuffix("```")
                                    )
                                } else {
                                    actor.answer(codeRequest, api = super.api)
                                }
                                displayCodeFeedback(
                                    task,
                                    actor,
                                    super.append(codeRequest, codeResponse),
                                    codeResponse,
                                    onComplete
                                )
                            } catch (e: Throwable) {
                                log.warn("Error", e)
                                val error = task.error(super.ui, e)
                                var regenButton: StringBuilder? = null
                                regenButton = task.complete(super.ui.hrefLink("♻", "href-link regen-button") {
                                    regenButton?.clear()
                                    val header = task.header("Regenerating...")
                                    super.displayCode(task, codeRequest)
                                    header?.clear()
                                    error?.clear()
                                    task.complete()
                                })
                            }
                        } catch (e: Throwable) {
                            log.warn("Error", e)
                            task.error(ui, e)
                        }
                    }
                }
            }
      """.trimMargin(), className = "reply-message"
        )
        formText.append(formHandle.toString())
        formHandle.toString()
        task.complete()
    }


    class ServletBuffer : ArrayList<HttpServlet>()

    private fun buildTestPage(
        openAPI: OpenAPI,
        servletImpl: String,
        task: SessionTask
    ) {
        var testPage = SimpleActor(
            prompt = "Given the definition for a servlet handler, create a test page that can be used to test the servlet",
            model = model,
        ).answer(
            listOf(
                JsonUtil.toJson(openAPI),
                servletImpl
            ), api
        )
        // if ```html unwrap
        if (testPage.contains("```html")) testPage = testPage.substringAfter("```html").substringBefore("```")
        task.add(renderMarkdown("```html\n$testPage\n```", ui = ui))
        task.complete(
            "<a href='${
                task.saveFile(
                    "test.html",
                    testPage.toByteArray(Charsets.UTF_8)
                )
            }'>Test Page</a> for  ${openAPI.paths?.entries?.first()?.key ?: "unknown"} Saved"
        )
    }

    abstract fun getInterpreterString(): String

    private fun answer(
        actor: CodingActor,
        request: CodingActor.CodeRequest,
        task: SessionTask = ui.newTask(),
        feedback: Boolean = true,
    ): CodeResult {
        val response = actor.answer(request, api = api)
        if (feedback) displayCodeAndFeedback(task, request, response)
        else displayCode(task, response)
        return response
    }

    companion object {
        val log = LoggerFactory.getLogger(ToolAgent::class.java)
        fun <T> execWrap(fn: () -> T): T {
            val classLoader = Thread.currentThread().contextClassLoader
            val prevCL = KotlinInterpreter.classLoader
            KotlinInterpreter.classLoader = classLoader //req.javaClass.classLoader
            return try {
                WebAppClassLoader.runWithServerClassAccess {
                    require(null != classLoader.loadClass("org.eclipse.jetty.server.Response"))
                    require(null != classLoader.loadClass("org.eclipse.jetty.server.Request"))
                    // com.simiacryptus.jopenai.OpenAIClient
                    require(null != classLoader.loadClass("com.simiacryptus.jopenai.OpenAIClient"))
                    require(null != classLoader.loadClass("com.simiacryptus.jopenai.API"))
                    fn()
                }
            } finally {
                KotlinInterpreter.classLoader = prevCL
            }
        }
    }
}