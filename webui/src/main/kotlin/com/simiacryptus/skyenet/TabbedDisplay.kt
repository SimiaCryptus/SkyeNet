package com.simiacryptus.skyenet

import com.simiacryptus.skyenet.webui.session.SessionTask

open class TabbedDisplay(
  val task: SessionTask,
  val tabs: MutableList<Pair<String, StringBuilder>> = mutableListOf(),
) {
  companion object {
    val log = org.slf4j.LoggerFactory.getLogger(TabbedDisplay::class.java)
//    val scheduledPool = java.util.concurrent.Executors.newScheduledThreadPool(1)
  }
  val size: Int get() = tabs.size
  open fun render() = """
    <div class="tabs-container">
      ${renderTabButtons()}
      ${
        tabs.withIndex().joinToString("\n")
        { (idx, t) -> renderContentTab(t, idx) }
      }
      </div>
    """.trimIndent()

  val container = task.add(render())

  open fun renderTabButtons() = """
    <div class="tabs">${
      tabs.toMap().keys.withIndex().joinToString("\n") { (idx, key: String) ->
        """<button class="tab-button" data-for-tab="$idx">$key</button>"""
      }
    }</div>
    """.trimIndent()

  open fun renderContentTab(t: Pair<String, StringBuilder>, idx: Int) = """
    <div class="tab-content ${
      when {
        idx == size - 1 -> "active"
        else -> ""
      }
    }" data-tab="$idx">${t.second}</div>""".trimIndent()


  operator fun get(i: String) = tabs.toMap()[i]
  operator fun set(name: String, content: String) =
    when (val index = find(name)?.let { it + 1 }) {
      null -> {
        val stringBuilder = StringBuilder(content)
        tabs.add(name to stringBuilder)
        update()
        stringBuilder
      }

      else -> {
        val stringBuilder = tabs[index].second
        stringBuilder.clear()
        stringBuilder.append(content)
        update()
        stringBuilder
      }
    }

  fun find(name: String) = tabs.withIndex().firstOrNull { it.value.first == name }?.index

  open fun label(i: Int): String {
    return when {
      tabs.size <= i -> "Tab ${tabs.size + 1}"
      else -> tabs[i].first
    }
  }

  fun clear() {
    tabs.clear()
    update()
  }

  fun update() {
    if(container != null) synchronized(container) {
      container.clear()
      container.append(render())
    }
    task.complete()
  }
}