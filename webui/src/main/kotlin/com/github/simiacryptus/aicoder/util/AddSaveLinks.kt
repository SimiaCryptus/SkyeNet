package com.github.simiacryptus.aicoder.util

import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.session.SocketManagerBase

fun SocketManagerBase.addSaveLinks(
  response: String,
  task: SessionTask,
  handle: (String, String) -> Unit
): String {
  val diffPattern =
    """(?s)(?<![^\n])#+\s*(?:[^\n]+[:\-]\s+)?([^\n]+)(?:[^`]+`?)*\n```[^\n]*\n(.*?)```""".toRegex() // capture filename
  val matches = diffPattern.findAll(response).distinct()
  val withLinks = matches.fold(response) { markdown, diffBlock ->
    val filename = diffBlock.groupValues[1]
    val codeValue = diffBlock.groupValues[2]
    val hrefLink = hrefLink("Save File") {
      try {
        handle(filename, codeValue)
        task.complete("""<div class="user-message">Saved ${filename}</div>""")
      } catch (e: Throwable) {
        task.error(null, e)
      }
    }
    markdown.replace(codeValue + "```", codeValue?.let { /*escapeHtml4*/(it)/*.indent("  ")*/ } + "```\n" + hrefLink)
  }
  return withLinks
}