package com.github.simiacryptus.diff

import com.simiacryptus.skyenet.set
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.session.SocketManagerBase

fun SocketManagerBase.addSaveLinks(
  response: String,
  task: SessionTask,
  ui: ApplicationInterface,
  handle: (String, String) -> Unit,
): String {
  val diffPattern =
    """(?s)(?<![^\n])#+\s*(?:[^\n]+[:\-]\s+)?([^\n]+)(?:[^`]+`?)*\n```[^\n]*\n(.*?)```""".toRegex() // capture filename
  val matches = diffPattern.findAll(response).distinct()
  val withLinks = matches.fold(response) { markdown, diffBlock ->
    val filename = diffBlock.groupValues[1]
    val codeValue = diffBlock.groupValues[2]
    val commandTask = ui.newTask(false)
    lateinit var hrefLink: StringBuilder
    hrefLink = commandTask.complete(hrefLink("Save File", classname = "href-link cmd-button") {
      try {
        handle(filename, codeValue)
        hrefLink.set("""<div class="cmd-button">Saved ${filename}</div>""")
        commandTask.complete()
        //task.complete("""<div class="cmd-button">Saved ${filename}</div>""")
      } catch (e: Throwable) {
        task.error(null, e)
      }
    })!!
    markdown.replace(codeValue + "```", codeValue?.let { /*escapeHtml4*/(it)/*.indent("  ")*/ } + "```\n" + commandTask.placeholder)
  }
  return withLinks
}