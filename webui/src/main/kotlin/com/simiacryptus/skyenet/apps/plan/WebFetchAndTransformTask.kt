package com.simiacryptus.skyenet.apps.plan

import org.jsoup.nodes.Node

private fun Node.text(): String {
  return this.childNodes().joinToString("") { it.text() }
}
