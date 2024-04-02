package com.simiacryptus.skyenet

import com.simiacryptus.skyenet.webui.application.ApplicationInterface

object AgentPatterns {
  private val scheduledThreadPoolExecutor = java.util.concurrent.Executors.newScheduledThreadPool(1)

  fun displayMapInTabs(
    map: Map<String, String>,
    ui: ApplicationInterface? = null,
    split: Boolean = map.entries.map { it.value.length + it.key.length }.sum() > 10000
  ) : String = if(split && ui != null) {
    val tasks = map.entries.map { (key, value) ->
      key to ui.newTask(root = false)
    }.toMap()
    scheduledThreadPoolExecutor.schedule({
      tasks.forEach { (key, task) ->
        task.complete(map[key]!!)
      }
    }, 200, java.util.concurrent.TimeUnit.MILLISECONDS)
    displayMapInTabs(tasks.mapValues { it.value.placeholder }, ui=ui, split = false)
  } else {
    """
    |<div class="tabs-container">
    |<div class="tabs">
    |${
      map.keys.joinToString("\n") { key ->
        """<button class="tab-button" data-for-tab="$key">$key</button>"""
      }/*.indent("  ")*/
    }
    |</div>
    |${
      map.entries.withIndex().joinToString("\n") { (idx, t) ->
        val (key, value) = t
        """
        |<div class="tab-content${
          when {
            idx == 0 -> " active"
            else -> ""
          }
        }" data-tab="$key">
      |${value/*.indent("  ")*/}
      |</div>
      """.trimMargin()
      }/*.indent("  ")*/
    }
    |</div>
  """.trimMargin()
  }
}