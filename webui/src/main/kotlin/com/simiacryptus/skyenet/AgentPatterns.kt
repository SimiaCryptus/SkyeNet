package com.simiacryptus.skyenet

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.skyenet.core.actors.BaseActor
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.util.MarkdownUtil
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean

object AgentPatterns {

  fun List<Pair<List<ApiModel.ContentPart>, ApiModel.Role>>.toMessageList(): Array<ApiModel.ChatMessage> =
    this.map { (content, role) ->
      ApiModel.ChatMessage(
        role = role,
        content = content
      )
    }.toTypedArray()

  fun <T : Any> iterate(
    ui: ApplicationInterface,
    userMessage: String,
    heading: String = MarkdownUtil.renderMarkdown(userMessage),
    initialResponse: (String) -> T,
    reviseResponse: (String, T, String) -> T,
    outputFn: (SessionTask, T) -> Unit = { task, design -> task.add(MarkdownUtil.renderMarkdown(design.toString())) },
  ): T {
    val task = ui.newTask()
    return try {
      task.echo(heading)
      var design = initialResponse(userMessage)
      outputFn(task, design)
      var textInputHandle: StringBuilder? = null
      val onAccept = Semaphore(0)
      var textInput: String? = null
      var acceptLink: String? = null
      var retryLink: String? = null
      val feedbackGuard = AtomicBoolean(false)
      val acceptGuard = AtomicBoolean(false)
      val retryGuard = AtomicBoolean(false)
      fun feedbackForm() = """
              |<div style="display: flex;flex-direction: column;">
              |${acceptLink!!}
              |${retryLink!!}
              |</div>
              |${textInput!!}
            """.trimMargin()
      textInput = ui.textInput { userResponse ->
        if (feedbackGuard.getAndSet(true)) return@textInput
        textInputHandle?.clear()
        task.echo(MarkdownUtil.renderMarkdown(userResponse))
        design = reviseResponse(userMessage, design, userResponse)
        outputFn(task, design)
        textInputHandle = task.complete(feedbackForm(), className = "reply-message")
        feedbackGuard.set(false)
      }
      acceptLink = ui.hrefLink("\uD83D\uDC4D") {
        if (acceptGuard.getAndSet(true)) return@hrefLink
        textInputHandle?.clear()
        task.complete()
        onAccept.release()
      }
      retryLink = ui.hrefLink("♻") {
        if (retryGuard.getAndSet(true)) return@hrefLink
        textInputHandle?.clear()
        task.echo(heading)
        design = initialResponse(userMessage)
        outputFn(task, design)
        textInputHandle = task.complete(feedbackForm(), className = "reply-message")
        feedbackGuard.set(false)
        acceptGuard.set(false)
        retryGuard.set(false)
      }
      textInputHandle = task.complete(feedbackForm(), className = "reply-message")
      onAccept.acquire()
      design
    } catch (e: Throwable) {
      task.error(ui, e)
      throw e
    }
  }



  fun <I : Any, T : Any> iterate(
    input: String,
    heading: String = MarkdownUtil.renderMarkdown(input),
    actor: BaseActor<I, T>,
    toInput: (String) -> I,
    api: API,
    ui: ApplicationInterface,
    outputFn: (SessionTask, T) -> Unit = { task, design -> task.add(MarkdownUtil.renderMarkdown(design.toString())) }
  ) = iterate(
    ui = ui,
    userMessage = input,
    heading = heading,
    initialResponse = { actor.answer(toInput(it), api = api) },
    reviseResponse = { userMessage: String, design: T, userResponse: String ->
      val input = toInput(userMessage)
      actor.respond(
        messages = actor.chatMessages(input) +
            listOf(
              design.toString().toContentList() to ApiModel.Role.assistant,
              userResponse.toContentList() to ApiModel.Role.user
            ).toMessageList(),
        input = input,
        api = api
      )
    },
    outputFn = outputFn
  )

}