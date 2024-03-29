package com.simiacryptus.skyenet

import com.simiacryptus.skyenet.core.actors.CodingActor.Companion.indent

object AgentPatterns {

  fun displayMapInTabs(
    map: Map<String, String>,
  ) = """
    |<div class="tabs-container">
    |<div class="tabs">
    |${
    map.keys.joinToString("\n") { key ->
      """<button class="tab-button" data-for-tab="$key">$key</button>"""
    }.indent("  ")}
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
      |${value.indent("  ")}
      |</div>
      """.trimMargin()
    }.indent("  ")
  }
    |</div>
  """.trimMargin()

}