package com.simiacryptus.skyenet.apps.coding

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.proxy.ValidatedObject
import com.simiacryptus.skyenet.core.Interpreter
import com.simiacryptus.skyenet.core.actors.ActorSystem
import com.simiacryptus.skyenet.core.actors.CodingActor
import com.simiacryptus.skyenet.core.actors.CodingActor.CodeResult
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.AuthorizationInterface.OperationType
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
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
  val actorMap: Map<ActorTypes, CodingActor> = mapOf(
    ActorTypes.CodingActor to CodingActor(interpreter, symbols = symbols, temperature = temperature)
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
      val codeRequest = CodingActor.CodeRequest(listOf(userMessage))
      displayCode(message, codeRequest)
    } catch (e: Throwable) {
      log.warn("Error", e)
      message.error(e)
    }
  }

  open fun displayCode(
    task: SessionTask,
    codeRequest: CodingActor.CodeRequest,
    response: CodeResult = actor.answer(codeRequest, api = api),
  ) {
    try {
      task.add(
        renderMarkdown(
          //language=Markdown
          """
          |```${actor.language.lowercase(Locale.getDefault())}
          |${response.code}
          |```
          """.trimMargin().trim()
        )
      )
      displayFeedback(task, codeRequest, response)
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
    var formHandle: StringBuilder? = null
    var playLink: StringBuilder? = null
    playLink = task.add(if (!canPlay) "" else {
      ui.hrefLink("▶", "href-link play-button") {
        formHandle?.clear()
        playLink?.clear()
        val header = task.header("Running...")
        try {
          val result = response.result
          val feedback = """
            |# Result
            |```
            |${result.resultValue}
            |```
            |
            |# Output
            |```text
            |${result.resultOutput}
            |```
          """.trimMargin()
          header?.clear()
          task.add(renderMarkdown(feedback))
          displayFeedback(task, revise(request, response, feedback), response)
        } catch (e: Throwable) {
          header?.clear()
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
          displayCode(task, revise(request, response, message))
        }
      }
    })
    formHandle = task.add(ui.textInput { feedback ->
      try {
        formHandle?.clear()
        playLink?.clear()
        task.echo(renderMarkdown(feedback))
        displayCode(task, revise(request, response, feedback))
      } catch (e: Throwable) {
        log.warn("Error", e)
        task.error(e)
      }
    })
    task.complete()
  }

  open fun revise(
    request: CodingActor.CodeRequest,
    response: CodeResult?,
    feedback: String
  ) = CodingActor.CodeRequest(
    messages = request.messages +
        listOf(
          response?.code,
          feedback
        ).filterNotNull().filter { it.isNotBlank() }
  )

  companion object {
    private val log = LoggerFactory.getLogger(CodingAgent::class.java)
  }
}
