package com.simiacryptus.skyenet

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.ApiModel.Role
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.skyenet.core.actors.BaseActor
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference

object AgentPatterns {


  fun displayMapInTabs(
    map: Map<String, String>,
  ) = """
    <div class="tabs-container">
    <div class="tabs">${
    map.keys.joinToString("\n") { key ->
      """<button class="tab-button" data-for-tab="$key">$key</button>"""
    }
  }</div>
    ${
    map.entries.withIndex().joinToString("\n") { (idx, t) ->
      val (key, value) = t
      """<div class="tab-content ${
        when {
          idx == 0 -> "active"
          else -> ""
        }
      }" data-tab="$key">$value</div>"""
    }
  }
    </div>
  """.trimIndent()

  fun retryable(
    ui: ApplicationInterface,
    task: SessionTask,
    process: (StringBuilder) -> String,
  ) = Retryable(ui, task, process).apply { addTab(ui, process(container!!)) }

  fun <T : Any> iterate(
    ui: ApplicationInterface,
    userMessage: String,
    heading: String = renderMarkdown(userMessage),
    initialResponse: (String) -> T,
    reviseResponse: (List<Pair<String, Role>>) -> T,
    outputFn: (T) -> String = { design -> renderMarkdown(design.toString()) },
    task: SessionTask
  ): T = Acceptable(
    task = task,
    userMessage = userMessage,
    initialResponse = initialResponse,
    outputFn = outputFn,
    ui = ui,
    reviseResponse = reviseResponse,
    heading = heading
  ).call()


  fun <I : Any, T : Any> iterate(
    input: String,
    heading: String = renderMarkdown(input),
    actor: BaseActor<I, T>,
    toInput: (String) -> I,
    api: API,
    ui: ApplicationInterface,
    outputFn: (T) -> String = { design -> renderMarkdown(design.toString()) },
    task: SessionTask
  ) = Acceptable(
    task = task,
    userMessage = input,
    initialResponse = { it: String -> actor.answer(toInput(it), api = api) },
    outputFn = outputFn,
    ui = ui,
    reviseResponse = { userMessages: List<Pair<String, Role>> ->
      actor.respond(
        messages = (userMessages.map { ApiModel.ChatMessage(it.second, it.first.toContentList()) }.toTypedArray()),
        input = toInput(input),
        api = api
      )
    },
    atomicRef = AtomicReference(),
    semaphore = Semaphore(0),
    heading = heading
  ).call()

}