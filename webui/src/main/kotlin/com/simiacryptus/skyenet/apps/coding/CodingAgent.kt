package com.simiacryptus.skyenet.apps.coding

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.proxy.ValidatedObject
import com.simiacryptus.skyenet.core.actors.ActorSystem
import com.simiacryptus.skyenet.core.actors.CodingActor
import com.simiacryptus.skyenet.core.actors.CodingActor.CodeResult
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.AuthorizationInterface.OperationType
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.interpreter.Interpreter
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.reflect.KClass

open class CodingAgent<T : Interpreter>(
  val api: API,
  dataStorage: StorageInterface,
  session: Session,
  user: User?,
  val ui: ApplicationInterface,
  val interpreter: KClass<T>,
  val symbols: Map<String, Any>,
  temperature: Double = 0.1,
  val details: String? = null,
  val model: ChatModels = ChatModels.GPT35Turbo,
  val actorMap: Map<ActorTypes, CodingActor> = mapOf(
    ActorTypes.CodingActor to CodingActor(interpreter, symbols = symbols, temperature = temperature, details = details, model = model)
  ),
) : ActorSystem<CodingAgent.ActorTypes>(actorMap, dataStorage, user, session) {
  enum class ActorTypes {
    CodingActor
  }

  open val actor by lazy {
    getActor(ActorTypes.CodingActor) as CodingActor
  }

  open val canPlay by lazy {
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
      val codeRequest = CodingActor.CodeRequest(listOf(userMessage to ApiModel.Role.user))
      displayCode(message, codeRequest)
    } catch (e: Throwable) {
      log.warn("Error", e)
      message.error(e)
    }
  }

  fun displayCode(
    task: SessionTask,
    codeRequest: CodingActor.CodeRequest,
  ) {
    try {
      val lastUserMessage = codeRequest.messages.last { it.second == ApiModel.Role.user }.first.trim()
      val codeResponse: CodeResult = if(lastUserMessage.startsWith("```")) {
        actor.CodeResultImpl(
          messages = actor.chatMessages(codeRequest),
          input=codeRequest,
          api = api as OpenAIClient,
          givenCode=lastUserMessage.removePrefix("```").removeSuffix("```")
        )
      } else {
        actor.answer(codeRequest, api = api)
      }
      displayCode(task, codeRequest, codeResponse)
    } catch (e: Throwable) {
      log.warn("Error", e)
      val error = task.error(e)
      var regenButton: StringBuilder? = null
      regenButton = task.complete(ui.hrefLink("♻", "href-link regen-button") {
        regenButton?.clear()
        val header = task.header("Regenerating...")
        displayCode(task, codeRequest)
        header?.clear()
        error?.clear()
        task.complete()
      })
    }
  }
  protected open fun displayCode(
    task: SessionTask,
    codeRequest: CodingActor.CodeRequest,
    response: CodeResult /*= actor.answer(codeRequest, api = api)*/,
  ) {
    try {
      task.add(
        renderMarkdown(response.renderedResponse ?:
          //language=Markdown
          "```${actor.language.lowercase(Locale.getDefault())}\n${response.code.trim()}\n```"
        )
      )
      displayFeedback(task, CodingActor.CodeRequest(
        messages = codeRequest.messages +
            listOf(
              response.code to ApiModel.Role.assistant,
            ).filter { it.first.isNotBlank() }
      ), response)
    } catch (e: Throwable) {
      log.warn("Error", e)
      task.error(e)
    }
  }

  open fun displayFeedback(
    task: SessionTask,
    request: CodingActor.CodeRequest,
    response: CodeResult
  ) {
    var formText: String? = null
    var formHandle: StringBuilder? = null
    val playHandler: (t: Unit) -> Unit = {
      formHandle?.clear()
      val header = task.header("Running...")
      var revertButton: StringBuilder? = null
      try {
        execute(ui.newTask(), response, request)
      } finally {
        header?.clear()
        revertButton = task.complete(ui.hrefLink("↩", "href-link regen-button") {
          revertButton?.clear()
          formHandle?.append(formText)
          task.complete()
        })
      }
    }
    val regenHandler: (t: Unit) -> Unit = {
      formHandle?.clear()
      val header = task.header("Regenerating...")
      var revertButton: StringBuilder? = null
      try {
        displayCode(ui.newTask(), request.copy(messages = request.messages.dropLastWhile { it.second == ApiModel.Role.assistant }))
      } finally {
        header?.clear()
        revertButton = task.complete(ui.hrefLink("↩", "href-link regen-button") {
          revertButton?.clear()
          formHandle?.append(formText)
          task.complete()
        })
      }
    }
    val feedbackHandler: (t: String) -> Unit = { feedback ->
      formHandle?.clear()
      val header = task.header("Revising...")
      var revertButton: StringBuilder? = null
      try {
        feedback(ui.newTask(), feedback, request, response)
      } finally {
        header?.clear()
        revertButton = task.complete(ui.hrefLink("↩", "href-link regen-button") {
          revertButton?.clear()
          formHandle?.append(formText)
          task.complete()
        })
      }
    }
    formHandle = task.add(
      """
      |<div style="display: flex;flex-direction: column;">
      |${if (canPlay) ui.hrefLink("▶", "href-link play-button", playHandler) else ""}
      |${if (canPlay) ui.hrefLink("♻", "href-link regen-button", regenHandler) else ""}
      |</div>
      |${ui.textInput(feedbackHandler)}
    """.trimMargin(), className = "reply-message"
    )
    formText = formHandle.toString()
    task.complete()
  }

  private fun feedback(
    task: SessionTask,
    feedback: String,
    request: CodingActor.CodeRequest,
    response: CodeResult
  ) {
    try {
      task.echo(renderMarkdown(feedback))
      displayCode(task, CodingActor.CodeRequest(
        messages = request.messages +
            listOf(
              response.code to ApiModel.Role.assistant,
              feedback to ApiModel.Role.user,
            ).filter { it.first.isNotBlank() }.map { it.first to it.second }
      ))
    } catch (e: Throwable) {
      log.warn("Error", e)
      task.error(e)
    }
  }

  private fun execute(
    task: SessionTask,
    response: CodeResult,
    request: CodingActor.CodeRequest
  ) {
    try {
      val resultValue = response.result.resultValue
      val resultOutput = response.result.resultOutput
      val result = when {
        resultValue.isBlank() || resultValue.trim().lowercase() == "null" -> """
              |# Output
              |```text
              |${resultOutput}
              |```
              """.trimMargin()

        else -> """
              |# Result
              |```
              |$resultValue
              |```
              |
              |# Output
              |```text
              |${resultOutput}
              |```
              """.trimMargin()
      }
      task.add(renderMarkdown(result))
      displayFeedback(task, CodingActor.CodeRequest(
        messages = request.messages +
            listOf(
              "Running...\n\n$result" to ApiModel.Role.assistant,
            ).filter { it.first.isNotBlank() }
      ), response)
    } catch (e: Throwable) {
      val message = when {
        e is ValidatedObject.ValidationError -> renderMarkdown(e.message ?: "")
        e is CodingActor.FailedToImplementException -> renderMarkdown(
          """
                |**Failed to Implement** 
                |
                |${e.message}
                |
                |""".trimMargin()
        )

        else -> renderMarkdown(
          """
                |**Error `${e.javaClass.name}`**
                |
                |```text
                |${e.message}
                |```
                |""".trimMargin()
        )
      }
      task.add(message, true, "div", "error")
      displayCode(task, CodingActor.CodeRequest(
        messages = request.messages +
            listOf(
              response.code to ApiModel.Role.assistant,
              message to ApiModel.Role.system,
            ).filter { it.first.isNotBlank() }
      ))
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(CodingAgent::class.java)
  }
}
