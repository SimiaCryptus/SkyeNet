package com.github.simiacryptus.diff

import com.simiacryptus.skyenet.AgentPatterns
import com.simiacryptus.skyenet.set
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SocketManagerBase
import com.simiacryptus.skyenet.webui.util.MarkdownUtil
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.readText

fun SocketManagerBase.addApplyFileDiffLinks(
    root: Path,
    code: Map<String, String>,
    response: String,
    handle: (Map<String, String>) -> Unit,
    ui: ApplicationInterface,
): String {
    val headerPattern = """(?s)(?<![^\n])#+\s*([^\n]+)""".toRegex() // capture filename
    val diffPattern = """(?s)(?<![^\n])```diff\n(.*?)\n```""".toRegex() // capture filename
    val codeblockPattern = """(?s)(?<![^\n])```([^\n])(\n.*?\n)```""".toRegex() // capture filename
    val headers = headerPattern.findAll(response).map { it.range to it.groupValues[1] }.toList()
    val diffs: List<Pair<IntRange, String>> =
        diffPattern.findAll(response).map { it.range to it.groupValues[1] }.toList()
    val codeblocks = codeblockPattern.findAll(response).filter {
        when (it.groupValues[1]) {
            "diff" -> false
            else -> true
        }
    }.map { it.range to it }.toList()
    val withPatchLinks: String = diffs.fold(response) { markdown, diffBlock ->
        val header = headers.lastOrNull { it.first.endInclusive < diffBlock.first.start }
        val filename = header?.second ?: "Unknown"
        val diffVal = diffBlock.second
        val newValue = renderDiffBlock(root, filename, code, diffVal, handle, ui)
        markdown.replace("```diff\n$diffVal\n```", newValue)
    }
    val withSaveLinks = codeblocks.fold(withPatchLinks) { markdown, codeBlock ->
        val header = headers.lastOrNull { it.first.endInclusive < codeBlock.first.start }
        val filename = header?.second ?: "Unknown"
        val filepath = path(root, filename)
        val prevCode = load(filepath, root, code)
        val codeLang = codeBlock.second.groupValues[1]
        val codeValue = codeBlock.second.groupValues[2]
        val commandTask = ui.newTask(false)
        lateinit var hrefLink: StringBuilder
        hrefLink = commandTask.complete(hrefLink("Save File", classname = "href-link cmd-button") {
            try {
                handle(
                    mapOf(
                        filename to codeValue
                    )
                )
                hrefLink.set("""<div class="cmd-button">Saved ${filename}</div>""")
                commandTask.complete()
                //task.complete("""<div class="cmd-button">Saved ${filename}</div>""")
            } catch (e: Throwable) {
                commandTask.error(null, e)
            }
        })!!

        val codeblockRaw = """
      ```${codeLang}
      ${codeValue}
      ```
      """.trimIndent()
        markdown.replace(
            codeblockRaw, AgentPatterns.displayMapInTabs(
                mapOf(
                    "New" to MarkdownUtil.renderMarkdown(codeblockRaw, ui = ui),
                    "Old" to MarkdownUtil.renderMarkdown(
                        """
          |```${codeLang}
          |${prevCode}
          |```
          """.trimMargin(), ui = ui
                    ),
                    "Patch" to MarkdownUtil.renderMarkdown(
                        """
          |```diff
          |${
                            DiffUtil.formatDiff(
                                DiffUtil.generateDiff(
                                    prevCode.lines(),
                                    codeValue.lines()
                                )
                            )
                        }
          |```
          """.trimMargin(), ui = ui
                    ),
                )
            ) + "\n" + commandTask.placeholder
        )
    }
    return withSaveLinks
}


private fun SocketManagerBase.renderDiffBlock(
    root: Path,
    filename: String,
    code: Map<String, String>,
    diffVal: String,
    handle: (Map<String, String>) -> Unit,
    ui: ApplicationInterface
): String {
    val filepath = path(root, filename)
    val prevCode = load(filepath, root, code)
    val newCode = IterativePatchUtil.patch(prevCode, diffVal)
    val echoDiff = try {
        DiffUtil.formatDiff(
            DiffUtil.generateDiff(
                prevCode.lines(),
                newCode.lines()
            )
        )
    } catch (e: Throwable) {
        MarkdownUtil.renderMarkdown("```\n${e.stackTraceToString()}\n```", ui = ui)
    }

    val applydiffTask = ui.newTask(false)
    lateinit var hrefLink: StringBuilder
    lateinit var reverseHrefLink: StringBuilder
    hrefLink = applydiffTask.complete(hrefLink("Apply Diff", classname = "href-link cmd-button") {
        try {
            val relativize = try {
                root.relativize(filepath)
            } catch (e: Throwable) {
                filepath
            }
            handle(
                mapOf(
                    relativize.toString() to IterativePatchUtil.patch(
                        prevCode,
                        diffVal
                    )
                )
            )
            reverseHrefLink.clear()
            hrefLink.set("""<div class="cmd-button">Diff Applied</div>""")
            applydiffTask.complete()
        } catch (e: Throwable) {
            applydiffTask.error(null, e)
        }
    })!!
    reverseHrefLink = applydiffTask.complete(hrefLink("(Bottom to Top)", classname = "href-link cmd-button") {
        try {
            val reversedCodeMap = code.mapValues { (_, v) -> v.lines().reversed().joinToString("\n") }
            val reversedDiff = diffVal.lines().reversed().joinToString("\n")
            val newReversedCodeMap = reversedCodeMap.mapValues { (file, prevCode) ->
                if (filename == file) {
                    IterativePatchUtil.patch(prevCode, reversedDiff).lines().reversed().joinToString("\n")
                } else prevCode
            }
            handle(newReversedCodeMap)
            hrefLink.clear()
            reverseHrefLink.set("""<div class="cmd-button">Diff Applied (Bottom to Top)</div>""")
            applydiffTask.complete()
        } catch (e: Throwable) {
            applydiffTask.error(null, e)
        }
    })!!
    val diffTask = ui.newTask(root = false)
    val prevCodeTask = ui.newTask(root = false)
    val newCodeTask = ui.newTask(root = false)
    val patchTask = ui.newTask(root = false)
    val inTabs = AgentPatterns.displayMapInTabs(
        mapOf(
            "Diff" to (diffTask?.placeholder ?: ""),
            "Code" to (prevCodeTask?.placeholder ?: ""),
            "Preview" to (newCodeTask?.placeholder ?: ""),
            "Echo" to (patchTask?.placeholder ?: ""),
        )
    )
    SocketManagerBase.scheduledThreadPoolExecutor.schedule({
        diffTask?.complete(MarkdownUtil.renderMarkdown(/*escapeHtml4*/("```diff\n$diffVal\n```"), ui = ui))
        newCodeTask?.complete(
            MarkdownUtil.renderMarkdown(
                "# $filename\n\n```${filename.split('.').lastOrNull() ?: ""}\n${newCode}\n```",
                ui = ui
            )
        )
        prevCodeTask?.complete(
            MarkdownUtil.renderMarkdown(
                "# $filename\n\n```${filename.split('.').lastOrNull() ?: ""}\n${prevCode}\n```",
                ui = ui
            )
        )
        patchTask?.complete(MarkdownUtil.renderMarkdown("# $filename\n\n```diff\n  ${echoDiff}\n```", ui = ui))
    }, 100, TimeUnit.MILLISECONDS)
    val newValue = inTabs + "\n" + applydiffTask.placeholder
    return newValue
}


private fun load(
    filepath: Path?,
    root: Path,
    code: Map<String, String>
) = try {
    if (true != filepath?.toFile()?.exists()) {
        log.warn(
            """
          |File not found: $filepath
          |Root: ${root.toAbsolutePath()}
          |Files: 
          |${code.keys.joinToString("\n") { "* $it" }}
          """.trimMargin()
        )
        ""
    } else {
        filepath.readText(Charsets.UTF_8)
    }
} catch (e: Throwable) {
    log.error("Error reading file: $filepath", e)
    ""
}

private fun path(root: Path, filename: String): Path? {
    val filepath = try {
        findFile(root, filename) ?: root.resolve(filename)
    } catch (e: Throwable) {
        log.error("Error finding file: $filename", e)
        try {
            root.resolve(filename)
        } catch (e: Throwable) {
            log.error("Error resolving file: $filename", e)
            File(filename).toPath()
        }
    }
    return filepath
}

fun findFile(root: Path, filename: String): Path? {
    return try {
        when {
            /* filename is absolute */
            filename.startsWith("/") -> {
                val resolve = File(filename)
                if (resolve.exists()) resolve.toPath() else findFile(root, filename.removePrefix("/"))
            }
            /* win absolute */
            filename.indexOf(":\\") == 1 -> {
                val resolve = File(filename)
                if (resolve.exists()) resolve.toPath() else findFile(
                    root,
                    filename.removePrefix(filename.substring(0, 2))
                )
            }

            root.resolve(filename).toFile().exists() -> root.resolve(filename)
            null != root.parent && root != root.parent -> findFile(root.parent, filename)
            else -> null
        }
    } catch (e: Throwable) {
        log.error("Error finding file: $filename", e)
        null
    }
}

private val log = org.slf4j.LoggerFactory.getLogger(PatchUtil::class.java)