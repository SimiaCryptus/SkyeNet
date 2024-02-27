package com.simiacryptus.skyenet.apps.general

import com.github.simiacryptus.aicoder.util.addApplyDiffLinks
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.proxy.ValidatedObject
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.AgentPatterns
import com.simiacryptus.skyenet.AgentPatterns.iterate
import com.simiacryptus.skyenet.AgentPatterns.toMessageList
import com.simiacryptus.skyenet.core.actors.ActorSystem
import com.simiacryptus.skyenet.core.actors.BaseActor
import com.simiacryptus.skyenet.core.actors.ParsedActor
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.platform.ClientManager
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.servlet.ToolServlet
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import org.slf4j.LoggerFactory

open class WebDevApp(
        applicationName: String = "Web Dev Assistant v1.0",
        open val symbols: Map<String, Any> = mapOf(),
        val temperature: Double = 0.1,
) : ApplicationServer(
    applicationName = applicationName,
    path = "/webdev",
) {
    override fun userMessage(
      session: Session,
      user: User?,
      userMessage: String,
      ui: ApplicationInterface,
      api: API
    ) {
        val settings = getSettings(session, user) ?: Settings()
        (api as ClientManager.MonitoredClient).budget = settings.budget ?: 2.00
        WebDevAgent(
          api = api,
          dataStorage = dataStorage,
          session = session,
          user = user,
          ui = ui,
          tools = settings.tools,
          model = settings.model,
        ).start(
            userMessage = userMessage,
        )
    }

    data class Settings(
      val budget: Double? = 2.00,
      val tools : List<String> = emptyList(),
      val model : ChatModels = ChatModels.GPT35Turbo,
    )

    override val settingsClass: Class<*> get() = Settings::class.java
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> initSettings(session: Session): T? = Settings() as T
}

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
    ActorTypes.ArchitectureDiscussionActor to SimpleActor(
      prompt = """
    Translate the user's idea into a detailed architecture for a simple web application. 
    Suggesting specific frameworks/libraries to import, and draft a basic structure for the project. 
    Identify key HTML classes and element IDs that will be used to bind the application to the HTML.
    Identify javascript libraries and provide CDN links for them.
    List Javascript files to be created, and for each file, describe the functions and interface.
    """.trimIndent(), model = model
    ),
    ActorTypes.CodeReviewer to SimpleActor(
      prompt = """
        Analyze the code summarized in the user's header-labeled code blocks.
        Review, look for bugs, and provide fixes. 
        Provide implementations for missing functions.
        
        Response should use one or more code patches in diff format within ```diff code blocks.
        Each diff should be preceded by a header that identifies the file being modified.
        The diff format should use + for line additions, - for line deletions.
        The diff should include 2 lines of context before and after every change.
        
        Example:
        
        Explanation text
        
        ### scripts/filename.js
        ```diff
        - const b = 2;
        + const a = 1;
        ```
        
        Continued text
      """.trimIndent(),
      model = model,
    ),
  ),
) : ActorSystem<WebDevAgent.ActorTypes>(actorMap, dataStorage, user, session) {
  enum class ActorTypes {
    HtmlCodingActor,
    JavascriptCodingActor,
    CssCodingActor,
    ResourceListParser,
    ArchitectureDiscussionActor,
    CodeReviewer,
  }

  val architectureDiscussionActor by lazy { getActor(ActorTypes.ArchitectureDiscussionActor) as SimpleActor }
  val htmlActor by lazy { getActor(ActorTypes.HtmlCodingActor) as SimpleActor }
  val javascriptActor by lazy { getActor(ActorTypes.JavascriptCodingActor) as SimpleActor }
  val cssActor by lazy { getActor(ActorTypes.CssCodingActor) as SimpleActor }
  val resourceListParser by lazy { getActor(ActorTypes.ResourceListParser) as ParsedActor<PageResourceList> }
  val codeReviewer by lazy { getActor(ActorTypes.CodeReviewer) as SimpleActor }

  val codeFiles = mutableMapOf<String, String>()

  fun start(
    userMessage: String,
  ) {
    val message = ui.newTask()
    try {
      message.echo(renderMarkdown(userMessage))

      val architectureResponse = AgentPatterns.iterate(
        input = userMessage,
        actor = architectureDiscussionActor,
        toInput = { listOf(it) },
        api = api,
        ui = ui,
      )
      message.add(renderMarkdown("### Architecture Discussion\n$architectureResponse"))

      val toolSpecs = tools.map { ToolServlet.tools.find { t -> t.path == it } }
        .joinToString("\n\n") { it?.let { JsonUtil.toJson(it.openApiDescription) } ?: "" }
      var messageWithTools = userMessage
      if (toolSpecs.isNotBlank()) messageWithTools += "\n\nThese services are available:\n$toolSpecs"
      val codeRequest = htmlActor.chatMessages(
        listOf(
          messageWithTools,
          architectureResponse
        )
      )
      draftHtmlCode(message, codeRequest)
    } catch (e: Throwable) {
      log.warn("Error", e)
      message.error(ui, e)
    }
  }

  private fun draftHtmlCode(
    task: SessionTask,
    request: Array<ApiModel.ChatMessage>,
  ) {
    try {
      var html = htmlActor.respond(emptyList(), api, *request)
      if (html.contains("```html")) html = html.substringAfter("```html").substringBefore("```")
      try {
        task.add(renderMarkdown("```html\n$html\n```"))
        task.add("<a href='${task.saveFile("main.html", html.toByteArray(Charsets.UTF_8))}'>Main Page</a> Updated")
        codeFiles["main.html"] = html
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
                  task1.error(ui, e)
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
        task.error(ui, e)
        log.warn("Error", e)
      }
    } catch (e: Throwable) {
      log.warn("Error", e)
      val error = task.error(ui, e)
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
      var code = actor.respond(emptyList(), api, *request)
      languages.forEach { language ->
        if (code.contains("```$language")) code = code.substringAfter("```$language").substringBefore("```")
      }
      try {
        task.add(renderMarkdown("```${languages.first()}\n$code\n```"))
        task.add("<a href='${task.saveFile(path, code.toByteArray(Charsets.UTF_8))}'>$path</a> Updated")
        codeFiles[path] = code
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
      } catch (e: Throwable) {
        task.error(ui, e)
        log.warn("Error", e)
      }
    } catch (e: Throwable) {
      log.warn("Error", e)
      val error = task.error(ui, e)
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
            javascriptActor.chatMessages(
              listOf(
                userPrompt,
                html,
                "Render $path - $description"
              )
            ),
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

      // Apply codeReviewer
      fun codeSummary() = codeFiles.entries.joinToString("\n\n") { (path, code) ->
        "# $path\n```${
          path.split('.').last()
        }\n$code\n```"
      }
      iterate(
        ui = ui,
        userMessage = codeSummary(),
        heading = renderMarkdown(codeSummary()),
        initialResponse = { codeReviewer.answer(listOf(it), api = api) },
        reviseResponse = { userMessage: String, design: String, userResponse: String ->
          codeReviewer.respond(
            messages = codeReviewer.chatMessages(listOf(codeSummary())) +
                listOf(
                  userResponse.toContentList() to ApiModel.Role.user
                ).toMessageList(),
            input = listOf(userMessage),
            api = api
          )
        },
        outputFn = { task: SessionTask, design: String ->
          task.add(renderMarkdown(ui.socketManager.addApplyDiffLinks(codeFiles, design) { newCodeMap ->
            newCodeMap.forEach { path, newCode ->
              val prev = codeFiles[path]
              if (prev != newCode) {
                codeFiles[path] = newCode
                task.add("<a href='${task.saveFile(path, newCode.toByteArray(Charsets.UTF_8))}'>$path</a> Updated")
              }
            }
          }))
        }
      )
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
