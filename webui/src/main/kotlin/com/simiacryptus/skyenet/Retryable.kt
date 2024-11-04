package com.simiacryptus.skyenet

import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask

open class Retryable(
  val ui: ApplicationInterface,
  task: SessionTask,
  val process: (StringBuilder) -> String
) : TabbedDisplay(task) {

  init {
    init()
  }

  open fun init() {
    val tabLabel = label(size)
    set(tabLabel, SessionTask.spinner)
    set(tabLabel, process(container))
  }

  override fun renderTabButtons(): String = """
<div class="tabs">${
    tabs.withIndex().joinToString("\n") { (index, _) ->
      val tabId = "$index"
      """<button class="tab-button" data-for-tab="$tabId">${index + 1}</button>"""
    }
  }
${
    ui.hrefLink("♻") {
      val idx = tabs.size
      val label = label(idx)
      val content = StringBuilder("Retrying..." + SessionTask.spinner)
      tabs.add(label to content)
      update()
      val newResult = process(content)
      content.clear()
      set(label, newResult)
    }
  }
</div>
"""

}
