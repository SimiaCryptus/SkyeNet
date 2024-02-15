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
    ActorTypes.JavascriptCodingActor to SimpleActor(
      prompt = """
      You will translate the user request into a javascript file for use in a rich javascript application.
    """.trimIndent(), model = model
    ),
    ActorTypes.CssCodingActor to SimpleActor(
      prompt = """
      You will translate the user request into a CSS file for use in a rich javascript application.
    """.trimIndent(), model = model
    ),
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
      draftHtmlCode(message, codeRequest)
    } catch (e: Throwable) {
      log.warn("Error", e)
      message.error(e)
    }
  }

  private fun draftHtmlCode(
    task: SessionTask,
    request: Array<ApiModel.ChatMessage>,
  ) {
    try {
      var html = htmlActor.respond(emptyList<String>(), api, *request)
      if (html.contains("```html")) html = html.substringAfter("```html").substringBefore("```")
      try {
        task.add(renderMarkdown("```html\n$html\n```"))
        task.add("<a href='${task.saveFile("main.html", html.toByteArray(Charsets.UTF_8))}'>Main Page</a> Updated")
        val request1 = append(request, html)
        val formText = StringBuilder()
        var formHandle: StringBuilder? = null
        formHandle = task.add(
          """
          |<div style="display: flex;flex-direction: column;">
          |${
            ui.hrefLink("♻", "href-link regen-button") {
              responseAction(task, "Regenerating...", formHandle!!, formText) {
                draftHtmlCode(
                  ui.newTask(),
                  request1.dropLastWhile { it.role == ApiModel.Role.assistant }.toTypedArray<ApiModel.ChatMessage>()
                )
              }
            }
          }
          |${generateResourcesButton(task, request1, html, formText) { formHandle!! }}
          |</div>
          |${
            ui.textInput { feedback ->
              responseAction(task, "Revising...", formHandle!!, formText) {
                val task1 = ui.newTask()
                try {
                  task1.echo(renderMarkdown(feedback))
                  draftHtmlCode(
                    task1, (request1.toList() + listOf(
                      html to ApiModel.Role.assistant,
                      feedback to ApiModel.Role.user,
                    ).filter { it.first.isNotBlank() }
                      .map {
                        ApiModel.ChatMessage(
                          it.second,
                          it.first.toContentList()
                        )
                      }).toTypedArray<ApiModel.ChatMessage>()
                  )
                } catch (e: Throwable) {
                  log.warn("Error", e)
                  task1.error(e)
                }
              }
            }
          }
          """.trimMargin(), className = "reply-message"
        )
        formText.append(formHandle.toString())
        formHandle.toString()
        task.complete()
      } catch (e: Throwable) {
        task.error(e)
        log.warn("Error", e)
      }
    } catch (e: Throwable) {
      log.warn("Error", e)
      val error = task.error(e)
      var regenButton: StringBuilder? = null
      regenButton = task.complete(ui.hrefLink("♻", "href-link regen-button") {
        regenButton?.clear()
        val header = task.header("Regenerating...")
        draftHtmlCode(task, request)
        header?.clear()
        error?.clear()
        task.complete()
      })
    }
  }

  private fun draftResourceCode(
    task: SessionTask,
    request: Array<ApiModel.ChatMessage>,
    actor: SimpleActor,
    path: String,
    vararg languages: String = arrayOf(path.split(".").last().lowercase()),
  ) {
    try {
      var code = actor.respond(emptyList<String>(), api, *request)
      languages.forEach { language ->
        if (code.contains("```$language")) code = code.substringAfter("```$language").substringBefore("```")
      }
      try {
        task.add(renderMarkdown("```${languages.first()}\n$code\n```"))
        task.add("<a href='${task.saveFile(path, code.toByteArray(Charsets.UTF_8))}'>$path</a> Updated")
        val request1 = append(request, code)
        val formText = StringBuilder()
        var formHandle: StringBuilder? = null
        formHandle = task.add(
          """
          |<div style="display: flex;flex-direction: column;">
          |${
            ui.hrefLink("♻", "href-link regen-button") {
              responseAction(task, "Regenerating...", formHandle!!, formText) {
                draftResourceCode(
                  ui.newTask(),
                  request1.dropLastWhile { it.role == ApiModel.Role.assistant }.toTypedArray<ApiModel.ChatMessage>(),
                  actor, path, *languages
                )
              }
            }
          }
          |</div>
          |${
            ui.textInput { feedback ->
              responseAction(task, "Revising...", formHandle!!, formText) {
                val task = ui.newTask()
                try {
                  task.echo(renderMarkdown(feedback))
                  draftResourceCode(
                    task, (request1.toList() + listOf(
                      code to ApiModel.Role.assistant,
                      feedback to ApiModel.Role.user,
                    ).filter { it.first.isNotBlank() }
                      .map {
                        ApiModel.ChatMessage(
                          it.second,
                          it.first.toContentList()
                        )
                      }).toTypedArray<ApiModel.ChatMessage>(), actor, path, *languages
                  )
                } catch (e: Throwable) {
                  log.warn("Error", e)
                  task.error(e)
                }
              }
            }
          }
          """.trimMargin(), className = "reply-message"
        )
        formText.append(formHandle.toString())
        formHandle.toString()
        task.complete()
      } catch (e: Throwable) {
        task.error(e)
        log.warn("Error", e)
      }
    } catch (e: Throwable) {
      log.warn("Error", e)
      val error = task.error(e)
      var regenButton: StringBuilder? = null
      regenButton = task.complete(ui.hrefLink("♻", "href-link regen-button") {
        regenButton?.clear()
        val header = task.header("Regenerating...")
        draftResourceCode(task, request, actor, path, *languages)
        header?.clear()
        error?.clear()
        task.complete()
      })
    }
  }

  fun append(
    codeRequest: Array<ApiModel.ChatMessage>,
    response: String
  ) = (codeRequest.toList() +
      listOf(
        ApiModel.ChatMessage(ApiModel.Role.assistant, response.toContentList()),
      )).toTypedArray()


  private fun generateResourcesButton(
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
      resources.resources.filter {
        !it.path.startsWith("http")
      }.forEach { (path, description) ->
        when (path.split(".").last().lowercase()) {

          "js" -> draftResourceCode(
            task,
            javascriptActor.chatMessages(listOf(
              userPrompt,
              html,
              "Render $path - $description"
            )),
            javascriptActor,
            path, "js", "javascript"
          )

          "css" -> draftResourceCode(
            task,
            cssActor.chatMessages(
              listOf(
                userPrompt,
                html,
                "Render $path - $description"
              )
            ),
            cssActor,
            path
          )

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
      var revertButton: StringBuilder? = null
      revertButton = task.complete(ui.hrefLink("↩", "href-link regen-button") {
        revertButton?.clear()
        formHandle?.append(formText)
        task.complete()
      })
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
