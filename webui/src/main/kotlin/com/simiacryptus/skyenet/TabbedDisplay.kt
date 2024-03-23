package com.simiacryptus.skyenet

import com.simiacryptus.skyenet.webui.session.SessionTask

open class TabbedDisplay(
  val task: SessionTask,
  val tabs: MutableList<Pair<String, StringBuilder>> = mutableListOf(),
) {
  val size: Int get() = tabs.size
  val container by lazy { task.add(render()) }
  open fun render() = """
      <div class="tabs-container">
        <div class="tabs">${
          tabs.toMap().keys.joinToString("\n") { key: String ->
            """<button class="tab-button" data-for-tab="$key">$key</button>"""
          }
      }</div>
      ${
    tabs.withIndex().joinToString("\n") { (idx, t) ->
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

  fun removeAt(it: Int) {
    tabs.removeAt(it)
    update()
  }

  fun clear() {
    tabs.clear()
    update()
  }

  fun update() {
    container?.clear()
    container?.append(render())
    task.complete()
  }
}