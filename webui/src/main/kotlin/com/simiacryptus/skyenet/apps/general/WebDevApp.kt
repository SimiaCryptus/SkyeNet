package com.simiacryptus.skyenet.apps.general

import com.github.simiacryptus.aicoder.util.addApplyDiffLinks2
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.proxy.ValidatedObject
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.AgentPatterns
import com.simiacryptus.skyenet.AgentPatterns.iterate
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
import java.util.concurrent.atomic.AtomicBoolean

open class WebDevApp(
  applicationName: String = "Web Dev Assistant v1.1",
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
    val tools: List<String> = emptyList(),
    val model: ChatModels = ChatModels.GPT4Turbo,
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
  val model: ChatModels,
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
    ActorTypes.ArchitectureDiscussionActor to ParsedActor(
//      parserClass = PageResourceListParser::class.java,
      resultClass = PageResourceList::class.java,
      prompt = """
        Translate the user's idea into a detailed architecture for a simple web application. 
        Suggest specific frameworks/libraries to import and provide CDN links for them.
        Specify user interactions and how the application will respond to them.
        Identify key HTML classes and element IDs that will be used to bind the application to the HTML.
        Identify coding styles and patterns to be used.
        List all files to be created, and for each file, describe the public interface / purpose / content summary.
      """.trimIndent(),
      model = model,
      parsingModel = model,
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
) : ActorSystem<WebDevAgent.ActorTypes>(actorMap.map { it.key.name to it.value }.toMap(), dataStorage, user, session) {
  enum class ActorTypes {
    HtmlCodingActor,
    JavascriptCodingActor,
    CssCodingActor,
    ArchitectureDiscussionActor,
    CodeReviewer,
  }

  private val architectureDiscussionActor by lazy { getActor(ActorTypes.ArchitectureDiscussionActor) as ParsedActor<PageResourceList> }
  private val htmlActor by lazy { getActor(ActorTypes.HtmlCodingActor) as SimpleActor }
  private val javascriptActor by lazy { getActor(ActorTypes.JavascriptCodingActor) as SimpleActor }
  private val cssActor by lazy { getActor(ActorTypes.CssCodingActor) as SimpleActor }
  private val codeReviewer by lazy { getActor(ActorTypes.CodeReviewer) as SimpleActor }
  private val codeFiles = mutableMapOf<String, String>()

  fun start(
    userMessage: String,
  ) {
    val task = ui.newTask()
    val architectureResponse = iterate(
      input = userMessage,
      heading = userMessage,
      actor = architectureDiscussionActor,
      toInput = { listOf(it) },
      api = api,
      ui = ui,
      outputFn = { design ->
//        renderMarkdown("${design.text}\n\n```json\n${JsonUtil.toJson(design.obj)}\n```")
        AgentPatterns.displayMapInTabs(
          mapOf(
            "Text" to renderMarkdown(design.text),
            "JSON" to renderMarkdown("```json\n${JsonUtil.toJson(design.obj)}\n```"),
          )
        )
      },
      task = task
    )

    try {
      val toolSpecs = tools.map { ToolServlet.tools.find { t -> t.path == it } }
        .joinToString("\n\n") { it?.let { JsonUtil.toJson(it.openApiDescription) } ?: "" }
      var messageWithTools = userMessage
      if (toolSpecs.isNotBlank()) messageWithTools += "\n\nThese services are available:\n$toolSpecs"
      task.echo(renderMarkdown("```json\n${JsonUtil.toJson(architectureResponse.obj)}\n```"))
      architectureResponse.obj.resources.filter {
        !it.path!!.startsWith("http")
      }.forEach { (path, description) ->
        val task = ui.newTask()
        when (path!!.split(".").last().lowercase()) {

          "js" -> draftResourceCode(
            task,
            javascriptActor.chatMessages(
              listOf(
                messageWithTools,
                architectureResponse.text,
                "Render $path - $description"
              )
            ),
            javascriptActor,
            path!!, "js", "javascript"
          )

          "css" -> draftResourceCode(
            task,
            cssActor.chatMessages(
              listOf(
                messageWithTools,
                architectureResponse.text,
                "Render $path - $description"
              )
            ),
            cssActor,
            path
          )

          "html" -> draftResourceCode(
            task,
            htmlActor.chatMessages(
              listOf(
                messageWithTools,
                architectureResponse.text,
                "Render $path - $description"
              )
            ),
            htmlActor,
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

      fun outputFn(task: SessionTask, design: String): StringBuilder? {
        //val task = ui.newTask()
        return task.complete(renderMarkdown(ui.socketManager.addApplyDiffLinks2(codeFiles, design, handle = { newCodeMap ->
          newCodeMap.forEach { (path, newCode) ->
            val prev = codeFiles[path]
            if (prev != newCode) {
              codeFiles[path] = newCode
              task.complete("<a href='${task.saveFile(path, newCode.toByteArray(Charsets.UTF_8))}'>$path</a> Updated")
            }
          }
        }, task = task)))
      }
      try {
        var task = ui.newTask()
        task.add(message = renderMarkdown(codeSummary()))
        var design = codeReviewer.answer(listOf(element = codeSummary()), api = api)
        outputFn(task, design)
        var textInputHandle: StringBuilder? = null
        var textInput: String? = null
        val feedbackGuard = AtomicBoolean(false)
        textInput = ui.textInput { userResponse ->
          if (feedbackGuard.getAndSet(true)) return@textInput
          textInputHandle?.clear()
          task.complete()
          task = ui.newTask()
          task.echo(renderMarkdown(userResponse))
          val codeSummary = codeSummary()
          task.add(renderMarkdown(codeSummary))
          design = codeReviewer.respond(
            messages = codeReviewer.chatMessages(
              listOf(
                codeSummary,
                userResponse,
              )
            ),
            input = listOf(element = codeSummary),
            api = api
          )
          outputFn(task, design)
          textInputHandle = task.complete(textInput!!)
          feedbackGuard.set(false)
        }
        textInputHandle = task.complete(textInput)
      } catch (e: Throwable) {
        val task = ui.newTask()
        task.error(ui = ui, e = e)
        throw e
      }

    } catch (e: Throwable) {
      log.warn("Error", e)
      task.error(ui, e)
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
        val request1 = (request.toList() +
            listOf(
              ApiModel.ChatMessage(ApiModel.Role.assistant, code.toContentList()),
            )).toTypedArray<ApiModel.ChatMessage>()
        val formText = StringBuilder()
        var formHandle: StringBuilder? = null
        formHandle = task.add(
          """
          |<div style="display: flex;flex-direction: column;">
          |${
            ui.hrefLink("♻", "href-link regen-button"){
              val task = ui.newTask()
              responseAction(task, "Regenerating...", formHandle!!, formText) {
                draftResourceCode(
                  task,
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
                //val task = ui.newTask()
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
      regenButton = task.complete(ui.hrefLink("♻", "href-link regen-button"){
        regenButton?.clear()
        val header = task.header("Regenerating...")
        draftResourceCode(task, request, actor, path, *languages)
        header?.clear()
        error?.clear()
        task.complete()
      })
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
      revertButton = task.complete(ui.hrefLink("↩", "href-link regen-button"){
        revertButton?.clear()
        formHandle?.append(formText)
        task.complete()
      })
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(WebDevAgent::class.java)

    data class PageResourceList(
      @Description("List of resources in this project; don't forget the index.html file!")
      val resources: List<PageResource> = emptyList()
    ) : ValidatedObject {
      override fun validate(): String? = when {
        resources.isEmpty() -> "Resources are required"
        resources.any { it.validate() != null } -> "Invalid resource"
        else -> null
      }
    }

    data class PageResource(
      val path: String? = "",
      val description: String? = ""
    ) : ValidatedObject {
      override fun validate(): String? = when {
        path.isNullOrBlank() -> "Path is required"
        //path.contains(" ") -> "Path cannot contain spaces"
        //!path.contains(".") -> "Path must contain a file extension"
        else -> null
      }
    }

  }
}
