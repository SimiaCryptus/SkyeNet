package com.simiacryptus.skyenet

import com.simiacryptus.skyenet.webui.session.SessionTask

open class TabbedDisplay(
    val task: SessionTask,
    val tabs: MutableList<Pair<String, StringBuilder>> = mutableListOf(),
) {
    var selectedTab: Int = 0

    companion object {
        val log = org.slf4j.LoggerFactory.getLogger(TabbedDisplay::class.java)
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

    val container: StringBuilder by lazy { task.add(render())!! }

    open fun renderTabButtons() = """
    <div class="tabs">${
        tabs.withIndex().joinToString("\n") { (idx, pair) ->
            if (idx == selectedTab) {
                """<button class="tab-button active" data-for-tab="$idx">${pair.first}</button>"""
            } else {
                """<button class="tab-button" data-for-tab="$idx">${pair.first}</button>"""
            }
        }
    }</div>
    """.trimIndent()

    open fun renderContentTab(t: Pair<String, StringBuilder>, idx: Int) = """
    <div class="tab-content ${
        when {
            idx == selectedTab -> "active"
            else -> ""
        }
    }" data-tab="$idx">${t.second}</div>""".trimIndent()


    operator fun get(i: String) = tabs.toMap()[i]
    operator fun set(name: String, content: String) =
        when (val index = find(name)) {
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
        return "${tabs.size + 1}"
    }

    fun clear() {
        tabs.clear()
        update()
    }

    open fun update() {
        if (container != null) synchronized(container) {
            container.clear()
            container.append(render())
        }
        task.complete()
    }
}