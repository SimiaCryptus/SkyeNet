package com.simiacryptus.skyenet.apps.coding

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.describe.AbbrevWhitelistYamlDescriber
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.describe.TypeDescriber
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.core.actors.CodingActor
import com.simiacryptus.skyenet.core.actors.CodingActor.Companion.sortCode
import com.simiacryptus.skyenet.core.actors.ParsedActor
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.platform.*
import com.simiacryptus.skyenet.interpreter.Interpreter
import com.simiacryptus.skyenet.kotlin.KotlinInterpreter
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.servlet.Tool
import com.simiacryptus.skyenet.webui.servlet.ToolServlet
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.util.MarkdownUtil
import com.simiacryptus.skyenet.webui.util.OpenApi
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Response
import org.eclipse.jetty.webapp.WebAppClassLoader
import org.openapitools.codegen.SpecValidationException
import org.slf4j.LoggerFactory
import java.io.File
import java.util.function.Function
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
  model: ChatModels = ChatModels.GPT35Turbo,
  actorMap: Map<ActorTypes, CodingActor> = mapOf(
    ActorTypes.CodingActor to CodingActor(
      interpreter,
      symbols = symbols,
      temperature = temperature,
      details = details,
      model = model
    )
  ),
) : CodingAgent<T>(api, dataStorage, session, user, ui, interpreter, symbols, temperature, details, model, actorMap) {
  override fun displayFeedback(task: SessionTask, request: CodingActor.CodeRequest, response: CodingActor.CodeResult) {
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

  protected fun createToolButton(
    task: SessionTask,
    request: CodingActor.CodeRequest,
    response: CodingActor.CodeResult,
    formText: StringBuilder,
    formHandle: () -> StringBuilder
  ) = ui.hrefLink("\uD83D\uDCE4", "href-link regen-button") {
    responseAction(task, "Exporting...", formHandle(), formText) {
      val dataschemaHandler = dataschemaHandler(request, response, task)
      val servletHandler = servletHandler(request, response, dataschemaHandler, task)
      val servletImpl = (dataschemaHandler + servletHandler).sortCode()
      val toolsPrefix = "/tools"
      var openAPI = openApi(servletImpl, toolsPrefix, task)
      for (i in 0..5) {
        try {
          org.openapitools.codegen.OpenAPIGenerator.main(
            arrayOf(
              "generate",
              "-i",
              File.createTempFile("openapi", ".json").apply {
                writeText(JsonUtil.toJson(openAPI))
                deleteOnExit()
              }.absolutePath,
              "-g", "html2",
              "-o", File(dataStorage.getSessionDir(user, session), "openapi/html2").apply { mkdirs() }.absolutePath,
            )
          )
          task.add("Validated OpenAPI Descriptor - <a href='fileIndex/$session/openapi/html2/index.html'>Documentation Saved</a>");
          break;
        } catch (e: SpecValidationException) {
          val error = """
            |${e.message}
            |${e.errors.joinToString("\n") { "ERROR:" + it.toString() }}
            |${e.warnings.joinToString("\n") { "WARN:" + it.toString() }}
          """.trimIndent()
          task.add(MarkdownUtil.renderMarkdown("```\n${error}\n```"))
          openAPI = openApiFix(servletImpl, toolsPrefix, task, JsonUtil.toJson(openAPI), error)
        }
      }


      if(ApplicationServices.authorizationManager.isAuthorized(ToolAgent.javaClass, user, AuthorizationInterface.OperationType.Admin)) {
        ToolServlet.addTool(
          Tool(
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

  private fun dataschemaHandler(
    request: CodingActor.CodeRequest,
    response: CodingActor.CodeResult,
    task: SessionTask
  ) = answer(
    object : CodingActor(
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
    }, request.copy(
      messages = listOf(
        response.code to ApiModel.Role.assistant,
        "From the given code prototype, identify input out output data structures and generate Kotlin data classes to define this schema" to ApiModel.Role.user
      )
    ), task, feedback = false
  ).code

  class ServletBuffer : ArrayList<HttpServlet>()

  private fun servletHandler(
    request: CodingActor.CodeRequest,
    response: CodingActor.CodeResult,
    dataschemaHandler: String,
    task: SessionTask
  ): String {
    val returnBuffer = ServletBuffer()
    val answer = answer(
      object : CodingActor(
        interpreterClass = actor.interpreterClass,
        symbols = actor.symbols + mapOf(
          "returnBuffer" to returnBuffer,
          "json" to JsonUtil,
          "req" to Request(null, null),
          "resp" to Response(null, null),
        ),
        describer = object : AbbrevWhitelistYamlDescriber(
          "com.simiacryptus",
          "com.github.simiacryptus"
        ) {
          override fun describe(rawType: Class<in Nothing>, stackMax: Int): String = when (rawType) {
            Request::class.java -> describe(HttpServletRequest::class.java)
            Response::class.java -> describe(HttpServletResponse::class.java)
            else -> super.describe(rawType, stackMax)
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
      }, request.copy(
        messages = listOf(
          response.code to ApiModel.Role.assistant,
          "Reprocess this code prototype into a servlet using the given data schema. " +
          "The last line should instantiate the new servlet class and return it via the returnBuffer collection." to ApiModel.Role.user
        ),
        codePrefix = dataschemaHandler
      ), task, feedback = false
    )
    val classLoader = Thread.currentThread().contextClassLoader
    val prevCL = KotlinInterpreter.classLoader
    KotlinInterpreter.classLoader = classLoader //req.javaClass.classLoader
    try {
      WebAppClassLoader.runWithServerClassAccess<Any?> {
        require(null != classLoader.loadClass("org.eclipse.jetty.server.Response"))
        require(null != classLoader.loadClass("org.eclipse.jetty.server.Request"))

        task.add(MarkdownUtil.renderMarkdown("```kotlin\n${answer.code}\n```"))
      }
    } finally {
      KotlinInterpreter.classLoader = prevCL
    }
    return answer.code
  }

  private fun openApi(
    servletImpl: String,
    toolsPrefix: String,
    task: SessionTask
  ): OpenApi {
    val openAPI = object : ParsedActor<OpenApi>(
      parserClass = OpenApiParser::class.java,
      model = model,
      prompt = "You are a code documentation assistant. You will create the OpenAPI definition for a servlet handler written in kotlin",
    ) {
      override val describer: TypeDescriber
        get() = object : AbbrevWhitelistYamlDescriber(
          //"com.simiacryptus", "com.github.simiacryptus"
        ) {
          override val includeMethods: Boolean get() = false
        }
    }.getParser(api).apply(servletImpl).let { openApi ->
      openApi.copy(paths = openApi.paths?.mapKeys { toolsPrefix + it.key })
    }
    task.add(MarkdownUtil.renderMarkdown("```json\n${JsonUtil.toJson(openAPI)}\n```"))
    return openAPI
  }

  private fun openApiFix(
    servletImpl: String,
    toolsPrefix: String,
    task: SessionTask,
    openApiDraft: String,
    errorMessage: String
  ): OpenApi {
    val specActor = object : ParsedActor<OpenApi>(
      parserClass = OpenApiParser::class.java,
      model = model,
      prompt = "You are a code documentation assistant. You will create the OpenAPI definition for a servlet handler written in kotlin",
    ) {
      override val describer: TypeDescriber
        get() = object : AbbrevWhitelistYamlDescriber(
          //"com.simiacryptus", "com.github.simiacryptus"
        ) {
          override val includeMethods: Boolean get() = false
        }
    }

    val newSpec = specActor.answer(
      listOf(
        servletImpl,
        openApiDraft,
        errorMessage
      ), api
    ).obj.let { openApi ->
      openApi.copy(paths = openApi.paths?.mapKeys { toolsPrefix + it.key.removePrefix(toolsPrefix) })
    }
    task.add(MarkdownUtil.renderMarkdown("```json\n${JsonUtil.toJson(newSpec)}\n```"))
    return newSpec
  }

  private fun buildTestPage(
    openAPI: OpenApi,
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
    task.add(MarkdownUtil.renderMarkdown("```html\n$testPage\n```"))
    task.complete(
      "<a href='${
        task.saveFile(
          "test.html",
          testPage.toByteArray(Charsets.UTF_8)
        )
      }'>Test Page</a> for  ${openAPI.paths?.entries?.first()?.key ?: "unknown"} Saved"
    )
  }

  abstract fun getInterpreterString(): String;

  interface OpenApiParser : Function<String, OpenApi> {
    @Description("Extract OpenAPI spec")
    override fun apply(t: String): OpenApi
  }

  private fun answer(
    actor: CodingActor,
    request: CodingActor.CodeRequest,
    task: SessionTask = ui.newTask(),
    feedback: Boolean = true,
  ): CodingActor.CodeResult {
    val response = actor.answer(request, api = api)
    if (feedback) displayCodeAndFeedback(task, request, response)
    else displayCode(task, response)
    return response
  }

  companion object {
    val log = LoggerFactory.getLogger(ToolAgent::class.java)
  }
}