package com.simiacryptus.skyenet

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.skyenet.core.actors.BaseActor
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

object AgentPatterns {

  fun retryable(
    ui: ApplicationInterface,
    task: SessionTask = ui.newTask(),
    process: () -> String,
  ): String {
    val container = task.add("<div class=\"tabs-container\"></div>")
    val history = mutableListOf<String>()
    return object {
      fun newHTML(ui: ApplicationInterface): String = """
      <div class="tabs-container">
        <div class="tabs">
        ${
        history.withIndex().joinToString("\n") { (index, _) ->
          val tabId = "$index"
          """<button class="tab-button" data-for-tab="$tabId">${index + 1}</button>"""
        }
      }
        ${
        ui.hrefLink("♻") {
          history.add("Retrying...")
          container?.clear()
          container?.append(newHTML(ui))
          task.add("")
          val newResult = process()
          history.removeLast()
          addTab(ui, newResult)
        }
      }
        </div>
        ${
        history.withIndex().joinToString("\n") { (index, content) ->
          """
            <div class="tab-content${if (index == history.size - 1) " active" else ""}" data-tab="$index">
              $content
            </div>
          """.trimIndent()
        }
      }
      </div>
    """.trimIndent()

      fun addTab(ui: ApplicationInterface, content: String): String {
        history.add(content)
        container?.clear()
        container?.append(newHTML(ui))
        task.complete()
        return content
      }
    }.addTab(ui, process())
  }

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
    heading: String = renderMarkdown(userMessage),
    initialResponse: (String) -> T,
    reviseResponse: (String, T, String) -> T,
    outputFn: (SessionTask, T) -> Unit = { task, design -> task.add(renderMarkdown(design.toString())) },
  ): T {
    val task = ui.newTask()
    fun main(): T = try {
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
        task.echo(renderMarkdown(userResponse))
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
      val atomicRef = AtomicReference<T>()
      val retryOnErrorLink = ui.hrefLink("🔄 Retry") {
        atomicRef.set(main())
      }
      task.error(ui, e)
      task.complete(retryOnErrorLink)
      atomicRef.get()
    }
    return main()
  }


  fun <I : Any, T : Any> iterate(
    input: String,
    heading: String = renderMarkdown(input),
    actor: BaseActor<I, T>,
    toInput: (String) -> I,
    api: API,
    ui: ApplicationInterface,
    outputFn: (SessionTask, T) -> Unit = { task, design -> task.add(renderMarkdown(design.toString())) }
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