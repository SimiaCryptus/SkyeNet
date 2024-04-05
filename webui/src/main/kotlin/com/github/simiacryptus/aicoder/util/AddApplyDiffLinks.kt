package com.github.simiacryptus.aicoder.util

import com.simiacryptus.skyenet.AgentPatterns.displayMapInTabs
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.session.SocketManagerBase
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown

fun SocketManagerBase.addApplyDiffLinks(
  code: StringBuilder,
  response: String,
  handle: (String) -> Unit,
  task: SessionTask,
  ui: ApplicationInterface? = null,
): String {
  val diffPattern = """(?s)(?<![^\n])```diff\n(.*?)\n```""".toRegex()
  val matches = diffPattern.findAll(response).distinct()
  val withLinks = matches.fold(response) { markdown, diffBlock ->
    val diffVal: String = diffBlock.groupValues[1]
    val hrefLink = hrefLink("Apply Diff") {
      try {
        val newCode = PatchUtil.patch(code.toString(), diffVal).replace("\r", "")
        handle(newCode)
        task.complete("""<div class="user-message">Diff Applied</div>""")
      } catch (e: Throwable) {
        task.error(ui, e)
      }
    }
    val reverseHrefLink = hrefLink("(Bottom to Top)") {
      try {
        val reversedCode = code.lines().reversed().joinToString("\n")
        val reversedDiff = diffVal.lines().reversed().joinToString("\n")
        val newReversedCode = PatchUtil.patch(reversedCode, reversedDiff).replace("\r", "")
        val newCode = newReversedCode.lines().reversed().joinToString("\n")
        handle(newCode)
        task.complete("""<div class="user-message">Diff Applied (Bottom to Top)</div>""")
      } catch (e: Throwable) {
        task.error(ui, e)
      }
    }
    val patch = PatchUtil.patch(code.toString(), diffVal).replace("\r", "")
    val test1 = DiffUtil.formatDiff(
      DiffUtil.generateDiff(
        code.toString().replace("\r", "").lines(),
        patch.lines()
      )
    )
    val patchRev = PatchUtil.patch(
      code.lines().reversed().joinToString("\n"),
      diffVal.lines().reversed().joinToString("\n")
    ).replace("\r", "")
    val test2 = DiffUtil.formatDiff(
      DiffUtil.generateDiff(
        code.lines(),
        patchRev.lines().reversed()
      )
    )
    val newValue = if (patchRev == patch) {
      displayMapInTabs(
        mapOf(
          "Diff" to renderMarkdown("```diff\n$diffVal\n```", ui = ui, tabs = true),
          "Verify" to renderMarkdown("```diff\n$test1\n```", ui = ui, tabs = true),
        ), ui = ui, split = true
      ) + "\n" + hrefLink
    } else {
      displayMapInTabs(
        mapOf(
          "Diff" to renderMarkdown("```diff\n$diffVal\n```", ui = ui, tabs = true),
          "Verify" to renderMarkdown("```diff\n$test1\n```", ui = ui, tabs = true),
          "Reverse" to renderMarkdown("```diff\n$test2\n```", ui = ui, tabs = true),
        ), ui = ui, split = true
      ) + "\n" + hrefLink + "\n" + reverseHrefLink
    }
    markdown.replace(diffBlock.value, newValue)
  }
  return withLinks
}