package com.simiacryptus.skyenet.apps.coding

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.skyenet.core.actors.CodingActor
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.interpreter.Interpreter
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask
import java.util.concurrent.Semaphore
import kotlin.reflect.KClass

open class CodingSubAgent<T : Interpreter>(
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
  val semaphore: Semaphore = Semaphore(0),
) : CodingAgent<T>(
  api = api,
  dataStorage = dataStorage,
  session = session,
  user = user,
  ui = ui,
  interpreter = interpreter,
  symbols = symbols,
  temperature = temperature,
  details = details,
  model = model,
  mainTask = mainTask
) {
  override fun displayFeedback(task: SessionTask, request: CodingActor.CodeRequest, response: CodingActor.CodeResult) {
    val formText = StringBuilder()
    var formHandle: StringBuilder? = null
    formHandle = task.add(
      """
      |<div style="display: flex;flex-direction: column;">
      |${if (!super.canPlay) "" else super.playButton(task, request, response, formText) { formHandle!! }}
      |${acceptButton(task, request, response, formText) { formHandle!! }}
      |</div>
      |${super.reviseMsg(task, request, response, formText) { formHandle!! }}
      """.trimMargin(), className = "reply-message"
    )
    formText.append(formHandle.toString())
    formHandle.toString()
    task.complete()
  }

  protected fun acceptButton(
    task: SessionTask,
    request: CodingActor.CodeRequest,
    response: CodingActor.CodeResult,
    formText: StringBuilder,
    formHandle: () -> StringBuilder
  ) = if (!canPlay) "" else
    ui.hrefLink("\uD83D\uDE80", "href-link play-button"){
      semaphore.release()
    }

}