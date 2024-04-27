package com.github.simiacryptus.diff

import com.simiacryptus.skyenet.AgentPatterns.displayMapInTabs
import com.simiacryptus.skyenet.set
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.session.SocketManagerBase
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown

fun SocketManagerBase.addApplyDiffLinks(
    code: () -> String,
    response: String,
    handle: (String) -> Unit,
    task: SessionTask,
    ui: ApplicationInterface,
): String {
    val diffPattern = """(?s)(?<![^\n])```diff\n(.*?)\n```""".toRegex()
    val matches = diffPattern.findAll(response).distinct()
    val withLinks = matches.fold(response) { markdown, diffBlock ->
        val diffVal: String = diffBlock.groupValues[1]
        val applydiffTask = ui.newTask(false)
        lateinit var hrefLink: StringBuilder
        var reverseHrefLink: StringBuilder? = null
        hrefLink = applydiffTask.complete(hrefLink("Apply Diff", classname = "href-link cmd-button") {
            try {
                val newCode = IterativePatchUtil.patch(code(), diffVal).replace("\r", "")
                handle(newCode)
                hrefLink.set("""<div class="cmd-button">Diff Applied</div>""")
                applydiffTask.complete()
                reverseHrefLink?.clear()
            } catch (e: Throwable) {
                hrefLink.append("""<div class="cmd-button">Error: ${e.message}</div>""")
                applydiffTask.complete()
                task.error(ui, e)
            }
        })!!
        val patch = IterativePatchUtil.patch(code(), diffVal).replace("\r", "")
        val test1 = DiffUtil.formatDiff(
            DiffUtil.generateDiff(
                code().replace("\r", "").lines(),
                patch.lines()
            )
        )
        val patchRev = IterativePatchUtil.patch(
            code().lines().reversed().joinToString("\n"),
            diffVal.lines().reversed().joinToString("\n")
        ).replace("\r", "")
        if (patchRev != patch) {
            reverseHrefLink = applydiffTask.complete(hrefLink("(Bottom to Top)", classname = "href-link cmd-button") {
                try {
                    val reversedCode = code().lines().reversed().joinToString("\n")
                    val reversedDiff = diffVal.lines().reversed().joinToString("\n")
                    val newReversedCode = IterativePatchUtil.patch(reversedCode, reversedDiff).replace("\r", "")
                    val newCode = newReversedCode.lines().reversed().joinToString("\n")
                    handle(newCode)
                    reverseHrefLink!!.set("""<div class="cmd-button">Diff Applied (Bottom to Top)</div>""")
                    applydiffTask.complete()
                    hrefLink.clear()
                } catch (e: Throwable) {
                    task.error(ui, e)
                }
            })!!
        }
        val test2 = DiffUtil.formatDiff(
            DiffUtil.generateDiff(
                code().lines(),
                patchRev.lines().reversed()
            )
        )
        val newValue = if (patchRev == patch) {
            displayMapInTabs(
                mapOf(
                    "Diff" to renderMarkdown("```diff\n$diffVal\n```", ui = ui, tabs = true),
                    "Verify" to renderMarkdown("```diff\n$test1\n```", ui = ui, tabs = true),
                ), ui = ui, split = true
            ) + "\n" + applydiffTask.placeholder
        } else {
            displayMapInTabs(
                mapOf(
                    "Diff" to renderMarkdown("```diff\n$diffVal\n```", ui = ui, tabs = true),
                    "Verify" to renderMarkdown("```diff\n$test1\n```", ui = ui, tabs = true),
                    "Reverse" to renderMarkdown("```diff\n$test2\n```", ui = ui, tabs = true),
                ), ui = ui, split = true
            ) + "\n" + applydiffTask.placeholder
        }
        markdown.replace(diffBlock.value, newValue)
    }
    return withLinks
}