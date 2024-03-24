package com.simiacryptus.skyenet

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


}