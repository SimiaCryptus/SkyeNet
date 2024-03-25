package com.simiacryptus.skyenet

import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask

open class Retryable(
  val ui: ApplicationInterface,
  val task: SessionTask,
  open val process: (StringBuilder) -> String,
) {
  val container = task.add("<div class=\"tabs-container\"></div>")
  private val history = mutableListOf<String>()
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
      val idx = history.size
      history.add("Retrying...")
      container?.clear()
      container?.append(newHTML(ui))
      task.add("")
      val newResult = process(container!!)
      addTab(ui, newResult, idx)
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

  fun addTab(ui: ApplicationInterface, content: String, idx: Int = history.size): String {
    if (idx < history.size) {
      history.set(idx, content)
    } else {
      history.add(content)
    }
    container?.clear()
    container?.append(newHTML(ui))
    task.complete()
    return content
  }
}