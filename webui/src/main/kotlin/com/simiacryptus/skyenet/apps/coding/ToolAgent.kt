package com.simiacryptus.skyenet.apps.coding

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.describe.AbbrevWhitelistYamlDescriber
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.describe.TypeDescriber
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.proxy.ValidatedObject
import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.core.actors.CodingActor
import com.simiacryptus.skyenet.core.actors.CodingActor.Companion.imports
import com.simiacryptus.skyenet.core.actors.CodingActor.Companion.sortCode
import com.simiacryptus.skyenet.core.actors.ParsedActor
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.interpreter.Interpreter
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.servlet.Tool
import com.simiacryptus.skyenet.webui.servlet.ToolServlet
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.util.MarkdownUtil
import com.simiacryptus.skyenet.webui.util.OpenApi
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Response
import org.slf4j.LoggerFactory
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
      val dataschemaHandler = answer(
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
      )
      val servletHandler = answer(
        object : CodingActor(
          interpreterClass = actor.interpreterClass,
          symbols = actor.symbols + mapOf(
            "json" to JsonUtil,
            "req" to Request(null,null),
            "resp" to Response(null,null),
          ),
          describer = object : AbbrevWhitelistYamlDescriber(
            "com.simiacryptus",
            "com.github.simiacryptus"
          ) {
            override fun describe(rawType: Class<in Nothing>, stackMax: Int): String = when(rawType) {
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
            "Reprocess this code prototype into a servlet method implementation that uses the given request and response objects and the given data schema" to ApiModel.Role.user
          ),
          codePrefix = dataschemaHandler.code
        ), task, feedback = false
      )
      val servletImpl = (dataschemaHandler.code + servletHandler.code).sortCode()
      val toolsPrefix = "/tools"
      val openAPI = object : ParsedActor<OpenApi>(
        parserClass = OpenApiParser::class.java,
        prompt = "You are a code documentation assistant. You will create the OpenAPI definition for a servlet handler written in kotlin",
      ) {
        override val describer: TypeDescriber
          get() = object : AbbrevWhitelistYamlDescriber(
            //"com.simiacryptus", "com.github.simiacryptus"
          ) {
            override val includeMethods: Boolean get() = false
          }
      }.answer(listOf(servletImpl), api).obj.let { openApi ->
        openApi.copy(paths = openApi.paths?.mapKeys { toolsPrefix + it.key })
      }
      task.add(MarkdownUtil.renderMarkdown("```json\n${JsonUtil.toJson(openAPI)}\n```"))
      val parsedServlet = ParsedActor(
        parserClass = ServletParser::class.java,
        prompt = "You are a code parsing assistant. You will extract the servlet handler definition from the given kotlin code",
      ).getParser(api).apply(servletHandler.code)
      task.add(MarkdownUtil.renderMarkdown("```json\n${JsonUtil.toJson(parsedServlet)}\n```"))
      var testPage = SimpleActor(
        prompt = "Given the definition for a servlet handler, create a test page that can be used to test the servlet",
      ).answer(listOf(
        JsonUtil.toJson(openAPI),
        servletImpl
      ), api)
      // if ```html unwrap
      if(testPage.contains("```html")) testPage = testPage.substringAfter("```html").substringBefore("```")
      task.add(MarkdownUtil.renderMarkdown("```html\n$testPage\n```"))
      ToolServlet.addTool(
        Tool(
          path = openAPI.paths?.entries?.first()?.key?.removePrefix(toolsPrefix) ?: "unknown",
          openApiDescription = openAPI,
          imports = (servletImpl.imports().joinToString("\n") + "\n" + dataschemaHandler.code).sortCode(),
          code = servletImpl,
          interpreterString = getInterpreterString(),
          testPage = testPage,
          parsedServlet = parsedServlet
        )
      )
      task.complete("<a href='${task.saveFile("test.html", testPage.toByteArray(Charsets.UTF_8))}'>Test Page</a> for  ${openAPI.paths?.entries?.first()?.key ?: "unknown"} Saved")
    }
  }

  abstract fun getInterpreterString(): String;

  interface ServletParser : Function<String, ServletInfo> {
    @Description("Extract Servlet Info")
    override fun apply(t: String): ServletInfo
  }

  data class ServletInfo(
    @Description("Post Handler or null if not present")
    val onPost : HandlerInfo? = null,
    @Description("Get Handler or null if not present")
    val onGet : HandlerInfo? = null,
    @Description("Put Handler or null if not present")
    val onPut : HandlerInfo? = null,
    @Description("Delete Handler or null if not present")
    val onDelete : HandlerInfo? = null,
  ) : ValidatedObject {
    override fun validate() = when {
      onPost == null && onGet == null && onPut == null && onDelete == null -> "At least one handler is required"
      onPost?.validate() != null -> "onPost: ${onPost.validate()}"
      onGet?.validate() != null -> "onGet: ${onGet.validate()}"
      onPut?.validate() != null -> "onPut: ${onPut.validate()}"
      onDelete?.validate() != null -> "onDelete: ${onDelete.validate()}"
      else -> null
    }
  }

  data class HandlerInfo(
    @Description("Verbatim method body for the handler; Do not include the method signature or the method name, just the body of the method.")
    val methodBody : String? = null,
    @Description("The symbol name of the request parameter, e.g. `req`")
    val requestName : String? = null,
    @Description("The symbol name of the response parameter, e.g. `resp`")
    val responseName : String? = null,
  ) : ValidatedObject {
    override fun validate() = when {
      requestName.isNullOrBlank() -> "requestParameterName is required"
      responseName.isNullOrBlank() -> "responseParameterName is required"
      methodBody.isNullOrBlank() -> "methodBody is required"
      else -> null
    }
  }


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
    if(feedback) displayCodeAndFeedback(task, request, response)
    else displayCode(task, response)
    return response
  }

  companion object {
    val log = LoggerFactory.getLogger(ToolAgent::class.java)
  }
}