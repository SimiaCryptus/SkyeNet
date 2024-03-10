package com.simiacryptus.skyenet.apps.coding

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.models.OpenAITextModel
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
  val model: ChatModels,
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
      val codeRequest = codeRequest(listOf(userMessage to ApiModel.Role.user))
      displayCode(message, codeRequest)
    } catch (e: Throwable) {
      log.warn("Error", e)
      message.error(ui, e)
    }
  }

  protected open fun codeRequest(messages: List<Pair<String, ApiModel.Role>>) =
    CodingActor.CodeRequest(messages)

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
      displayCodeAndFeedback(task, codeRequest, codeResponse)
    } catch (e: Throwable) {
      log.warn("Error", e)
      val error = task.error(ui, e)
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
  protected fun displayCodeAndFeedback(
    task: SessionTask,
    codeRequest: CodingActor.CodeRequest,
    response: CodeResult,
  ) {
    try {
      displayCode(task, response)
      displayFeedback(task, append(codeRequest, response), response)
    } catch (e: Throwable) {
      task.error(ui, e)
      log.warn("Error", e)
    }
  }

  fun append(
    codeRequest: CodingActor.CodeRequest,
    response: CodeResult
  ) = codeRequest(
    messages = codeRequest.messages +
        listOf(
          response.code to ApiModel.Role.assistant,
        ).filter { it.first.isNotBlank() }
  )

  fun displayCode(
    task: SessionTask,
    response: CodeResult
  ) {
    task.hideable(ui,
      renderMarkdown(
        response.renderedResponse ?:
        //language=Markdown
        "```${actor.language.lowercase(Locale.getDefault())}\n${response.code.trim()}\n```"
      )
    )
  }

  open fun displayFeedback(
    task: SessionTask,
    request: CodingActor.CodeRequest,
    response: CodeResult
  ) {
    val formText = StringBuilder()
    var formHandle: StringBuilder? = null
    formHandle = task.add(
      """
      |<div style="display: flex;flex-direction: column;">
      |${playButton(task, request, response, formText) { formHandle!! }}
      |${regenButton(task, request, formText) { formHandle!! }}
      |</div>
      |${reviseMsg(task, request, response, formText) { formHandle!! }}
      """.trimMargin(), className = "reply-message"
    )
    formText.append(formHandle.toString())
    formHandle.toString()
    task.complete()
  }


  protected fun reviseMsg(
    task: SessionTask,
    request: CodingActor.CodeRequest,
    response: CodeResult,
    formText: StringBuilder,
    formHandle: () -> StringBuilder
  ) = ui.textInput { feedback ->
    responseAction(task, "Revising...", formHandle(), formText) {
      feedback(ui.newTask(), feedback, request, response)
    }
  }

  protected fun regenButton(
    task: SessionTask,
    request: CodingActor.CodeRequest,
    formText: StringBuilder,
    formHandle: () -> StringBuilder
  ) = ui.hrefLink("♻", "href-link regen-button") {
    responseAction(task, "Regenerating...", formHandle(), formText) {
      displayCode(
        ui.newTask(),
        request.copy(messages = request.messages.dropLastWhile { it.second == ApiModel.Role.assistant })
      )
    }
  }

  protected fun playButton(
    task: SessionTask,
    request: CodingActor.CodeRequest,
    response: CodeResult,
    formText: StringBuilder,
    formHandle: () -> StringBuilder
  ) = if (!canPlay) "" else
    ui.hrefLink("▶", "href-link play-button") {
      responseAction(task, "Running...", formHandle(), formText) {
        execute(ui.newTask(), response, request)
      }
    }

  protected open fun responseAction(
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

  protected open fun revertButton(
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

  protected open fun feedback(
    task: SessionTask,
    feedback: String,
    request: CodingActor.CodeRequest,
    response: CodeResult
  ) {
    try {
      task.echo(renderMarkdown(feedback))
      displayCode(task, codeRequest(
        messages = request.messages +
            listOf(
              response.code to ApiModel.Role.assistant,
              feedback to ApiModel.Role.user,
            ).filter { it.first.isNotBlank() }.map { it.first to it.second }
      ))
    } catch (e: Throwable) {
      log.warn("Error", e)
      task.error(ui, e)
    }
  }

  protected open fun execute(
    task: SessionTask,
    response: CodeResult,
    request: CodingActor.CodeRequest,
  ) {
    try {
      val result = execute(task, response)
      displayFeedback(task, codeRequest(
        messages = request.messages +
            listOf(
              "Running...\n\n$result" to ApiModel.Role.assistant,
            ).filter { it.first.isNotBlank() }
      ), response)
    } catch (e: Throwable) {
      handleExecutionError(e, task, request, response)
    }
  }

  protected open fun handleExecutionError(
    e: Throwable,
    task: SessionTask,
    request: CodingActor.CodeRequest,
    response: CodeResult
  ) {
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

  fun execute(
    task: SessionTask,
    response: CodeResult
  ): String {
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
    return result
  }

  companion object {
    private val log = LoggerFactory.getLogger(CodingAgent::class.java)
  }
}
