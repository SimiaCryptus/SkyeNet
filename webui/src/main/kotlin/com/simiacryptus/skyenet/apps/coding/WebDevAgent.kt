package com.simiacryptus.skyenet.apps.coding

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.proxy.ValidatedObject
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.core.actors.ActorSystem
import com.simiacryptus.skyenet.core.actors.BaseActor
import com.simiacryptus.skyenet.core.actors.ParsedActor
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.AuthorizationInterface.OperationType
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.servlet.ToolServlet
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import org.slf4j.LoggerFactory

class WebDevAgent(
  val api: API,
  dataStorage: StorageInterface,
  session: Session,
  user: User?,
  val ui: ApplicationInterface,
  val model: ChatModels = ChatModels.GPT35Turbo,
  val tools: List<String> = emptyList(),
  val actorMap: Map<ActorTypes, BaseActor<*, *>> = mapOf(
    ActorTypes.HtmlCodingActor to SimpleActor(
      prompt = """
      You will translate the user request into a skeleton HTML file for a rich javascript application.
      The html file can reference needed CSS and JS files, which are will be located in the same directory as the html file.
      Do not output the content of the resource files, only the html file.
    """.trimIndent(), model = model
    ),
    ActorTypes.JavascriptCodingActor to SimpleActor(prompt = """
      You will translate the user request into a javascript file for use in a rich javascript application.
    """.trimIndent(), model = model),
    ActorTypes.CssCodingActor to SimpleActor(prompt = """
      You will translate the user request into a CSS file for use in a rich javascript application.
    """.trimIndent(), model = model),
    ActorTypes.ResourceListParser to ParsedActor(
      parserClass = PageResourceListParser::class.java,
      prompt = "Parse the page resource list",
      model = model,
    ),
  )
) : ActorSystem<WebDevAgent.ActorTypes>(actorMap, dataStorage, user, session) {
  enum class ActorTypes {
    HtmlCodingActor,
    JavascriptCodingActor,
    CssCodingActor,
    ResourceListParser,
  }

  val htmlActor by lazy { getActor(ActorTypes.HtmlCodingActor) as SimpleActor }
  val javascriptActor by lazy { getActor(ActorTypes.JavascriptCodingActor) as SimpleActor }
  val cssActor by lazy { getActor(ActorTypes.CssCodingActor) as SimpleActor }
  val resourceListParser by lazy { getActor(ActorTypes.ResourceListParser) as ParsedActor<PageResourceList> }

  private val canPlay by lazy {
    ApplicationServices.authorizationManager.isAuthorized(
      this::class.java,
      user,
      OperationType.Execute
    )
  }

  fun start(
    userMessage: String,
  ) {
    val message = ui.newTask()
    try {
      message.echo(renderMarkdown(userMessage))
      val toolSpecs = tools.map { ToolServlet.tools.find { t -> t.path == it } }
        .joinToString("\n\n") { it?.let { JsonUtil.toJson(it.openApiDescription) } ?: "" }
      var messageWithTools = userMessage
      if (toolSpecs.isNotBlank()) messageWithTools += "\n\nThese services are available:\n$toolSpecs"
      val codeRequest = htmlActor.chatMessages(listOf(messageWithTools))
      displayHtmlCode(message, codeRequest)
    } catch (e: Throwable) {
      log.warn("Error", e)
      message.error(e)
    }
  }

  private fun displayHtmlCode(
    task: SessionTask,
    request: Array<ApiModel.ChatMessage>,
  ) {
    try {
      val lastUserMessage = request.last { it.role == ApiModel.Role.user }.content?.first()?.text ?: ""
      var html = htmlActor.respond(emptyList<String>(), api, *request)
      if (html.contains("```html")) html = html.substringAfter("```html").substringBefore("```")
      displayCodeAndFeedback(task, request, html)
    } catch (e: Throwable) {
      log.warn("Error", e)
      val error = task.error(e)
      var regenButton: StringBuilder? = null
      regenButton = task.complete(ui.hrefLink("♻", "href-link regen-button") {
        regenButton?.clear()
        val header = task.header("Regenerating...")
        displayHtmlCode(task, request)
        header?.clear()
        error?.clear()
        task.complete()
      })
    }
  }

  private fun displayCodeAndFeedback(
    task: SessionTask,
    codeRequest: Array<ApiModel.ChatMessage>,
    response: String,
  ) {
    try {
      displayHtmlCode(task, response)
      displayFeedback(task, append(codeRequest, response), response)
    } catch (e: Throwable) {
      task.error(e)
      log.warn("Error", e)
    }
  }

  fun append(
    codeRequest: Array<ApiModel.ChatMessage>,
    response: String
  ) = (codeRequest.toList() +
      listOf(
        ApiModel.ChatMessage(ApiModel.Role.assistant, response.toContentList()),
      )).toTypedArray()

  private fun displayHtmlCode(
    task: SessionTask,
    response: String
  ) {
    task.add(renderMarkdown("```html\n$response\n```"))
    task.add("<a href='${task.saveFile("main.html", response.toByteArray(Charsets.UTF_8))}'>Main Page</a> Updated")
  }

  private fun displayFeedback(
    task: SessionTask,
    request: Array<ApiModel.ChatMessage>,
    response: String
  ) {
    val formText = StringBuilder()
    var formHandle: StringBuilder? = null
    formHandle = task.add(
      """
      |<div style="display: flex;flex-direction: column;">
      |${regenButton(task, request, formText) { formHandle!! }}
      |${continueButton(task, request, response, formText) { formHandle!! }}
      |</div>
      |${reviseMsg(task, request, response, formText) { formHandle!! }}
      """.trimMargin(), className = "reply-message"
    )
    formText.append(formHandle.toString())
    formHandle.toString()
    task.complete()
  }


  private fun reviseMsg(
    task: SessionTask,
    request: Array<ApiModel.ChatMessage>,
    response: String,
    formText: StringBuilder,
    formHandle: () -> StringBuilder
  ) = ui.textInput { feedback ->
    responseAction(task, "Revising...", formHandle(), formText) {
      feedback(ui.newTask(), feedback, request, response)
    }
  }

  private fun regenButton(
    task: SessionTask,
    request: Array<ApiModel.ChatMessage>,
    formText: StringBuilder,
    formHandle: () -> StringBuilder
  ) = ui.hrefLink("♻", "href-link regen-button") {
    responseAction(task, "Regenerating...", formHandle(), formText) {
      displayHtmlCode(
        ui.newTask(),
        request.dropLastWhile { it.role == ApiModel.Role.assistant }.toTypedArray()
      )
    }
  }

  private fun continueButton(
    task: SessionTask,
    request: Array<ApiModel.ChatMessage>,
    html: String,
    formText: StringBuilder,
    formHandle: () -> StringBuilder
  ) = ui.hrefLink("\uD83D\uDEE0\uFE0F", "href-link regen-button") {
    responseAction(task, "Generating Resources...", formHandle(), formText) {

      val userPrompt = request.first { it.role == ApiModel.Role.user }.content?.first()?.text ?: ""
      val resources = resourceListParser.getParser(api).apply(html)
      task.echo(renderMarkdown("```json\n${JsonUtil.toJson(resources)}\n```"))
      resources.resources.forEach { (path, description) ->
        when (path.split(".").last().lowercase()) {
          "js" -> {
            var js = javascriptActor.answer(
              listOf(
                userPrompt,
                html,
                "Render $path - $description"
              ), api
            )
            if (js.contains("```javascript")) js = js.substringAfter("```javascript").substringBefore("```")
            if (js.contains("```js")) js = js.substringAfter("```js").substringBefore("```")
            task.add(renderMarkdown("```javascript\n$js\n```"))
            task.add("<a href='${task.saveFile(path.removePrefix("/"), js.toByteArray(Charsets.UTF_8))}'>$path</a> Updated")
          }

          "css" -> {
            var css = cssActor.answer(
              listOf(
                userPrompt,
                html,
                "Render $path - $description"
              ), api
            )
            if (css.contains("```css")) css = css.substringAfter("```css").substringBefore("```")
            task.add(renderMarkdown("```css\n$css\n```"))
            task.add("<a href='${task.saveFile(path.removePrefix("/"), css.toByteArray(Charsets.UTF_8))}'>$path</a> Updated")
          }

          else -> task.add("Resource Type Not Supported: $path - $description")
        }
      }
    }
  }

  private fun responseAction(
    task: SessionTask,
    message: String,
    formHandle: StringBuilder?,
    formText: StringBuilder,
    fn: () -> Unit = {}
  ) {
    formHandle?.clear()
    val header = task.header(message)
    try {
      fn()
    } finally {
      header?.clear()
      revertButton(task, formHandle, formText)
    }
  }

  private fun revertButton(
    task: SessionTask,
    formHandle: StringBuilder?,
    formText: StringBuilder
  ): StringBuilder? {
    var revertButton: StringBuilder? = null
    revertButton = task.complete(ui.hrefLink("↩", "href-link regen-button") {
      revertButton?.clear()
      formHandle?.append(formText)
      task.complete()
    })
    return revertButton
  }

  private fun feedback(
    task: SessionTask,
    feedback: String,
    request: Array<ApiModel.ChatMessage>,
    response: String
  ) {
    try {
      task.echo(renderMarkdown(feedback))
      val map = listOf(
        response to ApiModel.Role.assistant,
        feedback to ApiModel.Role.user,
      ).filter { it.first.isNotBlank() }.map { ApiModel.ChatMessage(it.second, it.first.toContentList()) }
      val toTypedArray = (request.toList() +
          map).toTypedArray()
      displayHtmlCode(
        task, toTypedArray
      )
    } catch (e: Throwable) {
      log.warn("Error", e)
      task.error(e)
    }
  }

  interface PageResourceListParser : java.util.function.Function<String, PageResourceList> {
    @Description("Parse the page resource list")
    override fun apply(html: String): PageResourceList
  }

  data class PageResourceList(
    val resources: List<PageResource> = emptyList()
  ) : ValidatedObject {
    override fun validate(): String? = when {
      resources.isEmpty() -> "Resources are required"
      resources.any { it.validate() != null } -> "Invalid resource"
      else -> null
    }
  }

  data class PageResource(
    val path: String = "",
    val description: String = ""
  ) : ValidatedObject {
    override fun validate(): String? = when {
      path.isBlank() -> "Path is required"
      path.contains(" ") -> "Path cannot contain spaces"
      !path.contains(".") -> "Path must contain a file extension"
      description.isBlank() -> "Description is required"
      else -> null
    }
  }


  companion object {
    private val log = LoggerFactory.getLogger(WebDevAgent::class.java)
  }
}
