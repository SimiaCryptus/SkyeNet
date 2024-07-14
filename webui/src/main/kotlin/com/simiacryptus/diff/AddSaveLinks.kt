package com.simiacryptus.diff

import com.simiacryptus.skyenet.set
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.session.SocketManagerBase
import java.io.File
import java.nio.file.Path

fun SocketManagerBase.addSaveLinks(
    root: Path,
    response: String,
    task: SessionTask,
    ui: ApplicationInterface,
    shouldAutoApply: (Path) -> Boolean = { false },
): String {
    val diffPattern = """(?s)(?<![^\n])#+\s*([^\n]+)\n```[^\n]*\n(.*?)```""".toRegex()
    val matches = diffPattern.findAll(response).distinct()
    val withLinks = matches.fold(response) { markdown, diffBlock ->
        val filename1 = diffBlock.groupValues[1]
        val filename = when {
            pattern_backticks.containsMatchIn(filename1) -> {
                pattern_backticks.find(filename1)!!.groupValues[1]
            }

            else -> filename1.trim()
        }
        val filePath = File(filename).toPath()
        val codeValue = diffBlock.groupValues[2]
        if (shouldAutoApply(root.resolve(filePath))) {
            filePath.toFile().apply {
                parentFile.mkdirs()
                writeText(codeValue, Charsets.UTF_8)
            }
            markdown.replace(
                codeValue + "```",
                codeValue + "```\n<div class=\"cmd-button\">Auto-applied to ${filename}</div>"
            )
        } else {
            val commandTask = ui.newTask(false)
            lateinit var hrefLink: StringBuilder
            hrefLink = commandTask.complete(hrefLink("Save File", classname = "href-link cmd-button") {
                try {
                    filePath.toFile().apply {
                        parentFile.mkdirs()
                        writeText(codeValue, Charsets.UTF_8)
                    }
                    hrefLink.set("""<div class="cmd-button">Saved ${filename}</div>""")
                    commandTask.complete()
                } catch (e: Throwable) {
                    task.error(null, e)
                }
            })!!
            markdown.replace(
                codeValue + "```",
                codeValue + "```\n" + commandTask.placeholder
            )
        }
    }
    return withLinks
}


private val pattern_backticks = "`(.*)`".toRegex()