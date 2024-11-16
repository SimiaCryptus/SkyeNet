package com.simiacryptus.skyenet

import com.simiacryptus.skyenet.webui.session.SessionTask
import java.util.*

open class TabbedDisplay(
  val task: SessionTask,
  val tabs: MutableList<Pair<String, StringBuilder>> = mutableListOf(),
) {
  var selectedTab: Int = 0

  companion object {
    val log = org.slf4j.LoggerFactory.getLogger(TabbedDisplay::class.java)
  }

  val size: Int get() = tabs.size
  val tabId = UUID.randomUUID()
  open fun render() = if (tabs.isEmpty()) "<div/>" else {
    """
  <div class="tabs-container" id="$tabId">
  ${renderTabButtons()}
  ${
      tabs.toTypedArray().withIndex().joinToString("\n")
      { (idx, t) -> renderContentTab(t, idx) }
    }
  </div>
  """
  }

  val container: StringBuilder by lazy {
    log.debug("Initializing container with rendered content")
    task.add(render())!!
  }

  open fun renderTabButtons() = """
<div class="tabs">${
    tabs.toTypedArray().withIndex().joinToString("\n") { (idx, pair) ->
      if (idx == selectedTab) {
        """<button class="tab-button active" data-for-tab="$idx">${pair.first}</button>"""
      } else {
        """<button class="tab-button" data-for-tab="$idx">${pair.first}</button>"""
      }
    }
  }</div>
"""

  open fun renderContentTab(t: Pair<String, StringBuilder>, idx: Int) = """
<div class="tab-content ${
    when {
      idx == selectedTab -> "active"
      else -> ""
    }
  }" data-tab="$idx">${t.second}</div>"""


  operator fun get(i: String) = tabs.toMap()[i]
  operator fun set(name: String, content: String) =
    when (val index = find(name)) {
      null -> {
        log.debug("Adding new tab: $name")
        val stringBuilder = StringBuilder(content)
        tabs.add(name to stringBuilder)
        update()
        stringBuilder
      }

      else -> {
        log.debug("Updating existing tab: $name")
        val stringBuilder = tabs[index].second
        stringBuilder.clear()
        stringBuilder.append(content)
        update()
        stringBuilder
      }
    }

  fun find(name: String) = tabs.withIndex().firstOrNull { it.value.first == name }?.index

  open fun label(i: Int): String {
    return "${tabs.size + 1}"
  }

  open fun clear() {
    log.debug("Clearing all tabs")
    tabs.clear()
    update()
  }

  open fun update() {
    log.debug("Updating container content")
    synchronized(container) {
      if (tabs.isNotEmpty() && (selectedTab < 0 || selectedTab >= tabs.size)) {
        selectedTab = 0
      }
      container.clear()
      container.append(render())
    }
    task.complete()
  }

}