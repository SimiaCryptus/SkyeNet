package com.simiacryptus.diff

import com.simiacryptus.diff.IterativePatchUtil.patch
import com.simiacryptus.skyenet.AgentPatterns
import com.simiacryptus.skyenet.set
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.session.SocketManagerBase
import com.simiacryptus.skyenet.webui.util.MarkdownUtil
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.readText

fun SocketManagerBase.addApplyFileDiffLinks(
    root: Path,
    code: () -> Map<Path, String>,
    response: String,
    handle: (Map<Path, String>) -> Unit,
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
        val filename = resolve(root, header?.second ?: "Unknown")
        val diffVal = diffBlock.second
        val newValue = renderDiffBlock(root, filename, diffVal, handle, ui)
        markdown.replace("```diff\n$diffVal\n```", newValue)
    }
    val withSaveLinks = codeblocks.fold(withPatchLinks) { markdown, codeBlock ->
        val header = headers.lastOrNull { it.first.endInclusive < codeBlock.first.start }
        val filename = resolve(root, header?.second ?: "Unknown")
        val filepath: Path? = path(root, filename)
        val prevCode = load(filepath)
        val codeLang = codeBlock.second.groupValues[1]
        val codeValue = codeBlock.second.groupValues[2]
        val commandTask = ui.newTask(false)
        lateinit var hrefLink: StringBuilder
        hrefLink = commandTask.complete(hrefLink("Save File", classname = "href-link cmd-button") {
            try {
                save(filepath, codeValue)
                handle(mapOf(File(filename).toPath() to codeValue))
                hrefLink.set("""<div class="cmd-button">Saved ${filename}</div>""")
                commandTask.complete()
                //task.complete("""<div class="cmd-button">Saved ${filename}</div>""")
            } catch (e: Throwable) {
                hrefLink.append("""<div class="cmd-button">Error: ${e.message}</div>""")
                commandTask.error(null, e)
            }
        })!!

        val codeblockRaw = """
          |```${codeLang}
          |${codeValue}
          |```
          """.trimMargin()
        markdown.replace(
            codeblockRaw, AgentPatterns.displayMapInTabs(
                mapOf(
                    "New" to renderMarkdown(codeblockRaw, ui = ui),
                    "Old" to renderMarkdown(
                        """
                      |```${codeLang}
                      |${prevCode}
                      |```
                      """.trimMargin(), ui = ui
                    ),
                    "Patch" to renderMarkdown(
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

private val pattern_backticks = "`(.*)`".toRegex()
fun resolve(root: Path, filename: String): String {
    val filename = if (pattern_backticks.containsMatchIn(filename)) {
        pattern_backticks.find(filename)!!.groupValues[1]
    } else {
        filename.trim()
    }
    var filepath = path(root, filename)
    if (filepath?.toFile()?.exists() == false) filepath = null
    if (null != filepath) return filepath.toString()
    val files = root.toFile().recurseFiles().filter { it.name == filename.split('/', '\\').last() }
    if (files.size == 1) {
        filepath = files.first().toPath()
    }
    return filepath?.let { root.relativize(it).toString() } ?: filename
}

fun File.recurseFiles(): List<File> {
    val files = mutableListOf<File>()
    if (isDirectory) {
        listFiles()?.forEach {
            files.addAll(it.recurseFiles())
        }
    } else {
        files.add(this)
    }
    return files
}


private fun SocketManagerBase.renderDiffBlock(
    root: Path,
    filename: String,
    diffVal: String,
    handle: (Map<Path, String>) -> Unit,
    ui: ApplicationInterface
): String {

    val diffTask = ui.newTask(root = false)
    val prevCodeTask = ui.newTask(root = false)
    val prevCodeTaskSB = prevCodeTask.add("")
    val newCodeTask = ui.newTask(root = false)
    val newCodeTaskSB = newCodeTask.add("")
    val patchTask = ui.newTask(root = false)
    val patchTaskSB = patchTask.add("")
    val prevCode2Task = ui.newTask(root = false)
    val prevCode2TaskSB = prevCode2Task.add("")
    val newCode2Task = ui.newTask(root = false)
    val newCode2TaskSB = newCode2Task.add("")
    val patch2Task = ui.newTask(root = false)
    val patch2TaskSB = patch2Task.add("")
    val verifyFwdTabs = AgentPatterns.displayMapInTabs(
        mapOf(
            "Code" to (prevCodeTask?.placeholder ?: ""),
            "Preview" to (newCodeTask?.placeholder ?: ""),
            "Echo" to (patchTask?.placeholder ?: ""),
        )
    )
    val verifyRevTabs = AgentPatterns.displayMapInTabs(
        mapOf(
            "Code" to (prevCode2Task?.placeholder ?: ""),
            "Preview" to (newCode2Task?.placeholder ?: ""),
            "Echo" to (patch2Task?.placeholder ?: ""),
        )
    )
    val verifyTabs = AgentPatterns.displayMapInTabs(
        mapOf(
            "Forward" to verifyFwdTabs,
            "Reverse" to verifyRevTabs,
        )
    )
    val mainTabs = AgentPatterns.displayMapInTabs(
        mapOf(
            "Diff" to (diffTask?.placeholder ?: ""),
            "Verify" to verifyTabs,
        )
    )


    diffTask?.complete(renderMarkdown("```diff\n$diffVal\n```", ui = ui))


    val filepath = path(root, filename)
    val relativize = try {
        root.relativize(filepath)
    } catch (e: Throwable) {
        filepath
    }
    var filehash: Int? = 0

    val applydiffTask = ui.newTask(false)
    lateinit var hrefLink: StringBuilder
    var isApplied = false

    var originalCode = load(filepath)
    lateinit var revert: String
    val apply1 = hrefLink("Apply Diff", classname = "href-link cmd-button") {
        try {
            originalCode = load(filepath)
            val newCode = patch(originalCode, diffVal)
            filepath?.toFile()?.writeText(newCode, Charsets.UTF_8) ?: log.warn("File not found: $filepath")
            handle(mapOf(relativize!! to newCode))
            hrefLink.set("""<div class="cmd-button">Diff Applied</div>""" + revert)
            applydiffTask.complete()
            isApplied = true
        } catch (e: Throwable) {
            hrefLink.append("""<div class="cmd-button">Error: ${e.message}</div>""")
            applydiffTask.error(null, e)
        }
    }
    val apply2 = hrefLink("(Bottom to Top)", classname = "href-link cmd-button") {
        try {
            originalCode = load(filepath)
            val newCode2 = patch(
                originalCode.lines().reversed().joinToString("\n"),
                diffVal.lines().reversed().joinToString("\n")
            ).lines().reversed().joinToString("\n")
            filepath?.toFile()?.writeText(newCode2, Charsets.UTF_8) ?: log.warn("File not found: $filepath")
            handle(mapOf(relativize!! to newCode2))
            hrefLink.set("""<div class="cmd-button">Diff Applied (Bottom to Top)</div>""" + revert)
            applydiffTask.complete()
            isApplied = true
        } catch (e: Throwable) {
            hrefLink.append("""<div class="cmd-button">Error: ${e.message}</div>""")
            applydiffTask.error(null, e)
        }
    }
    revert = hrefLink("Revert", classname = "href-link cmd-button") {
        try {
            save(filepath, originalCode)
            handle(mapOf(relativize!! to originalCode))
            hrefLink.set("""<div class="cmd-button">Reverted</div>""" + apply1 + apply2)
            applydiffTask.complete()
            isApplied = false
        } catch (e: Throwable) {
            hrefLink.append("""<div class="cmd-button">Error: ${e.message}</div>""")
            applydiffTask.error(null, e)
        }
    }

    hrefLink = applydiffTask.complete(apply1 + "\n" + apply2)!!

    lateinit var scheduledFn: () -> Unit
    scheduledFn = {
        val thisFilehash = try {
            filepath?.toFile()?.readText().hashCode()
        } catch (e: Throwable) {
            null
        }
        if (!isApplied) {
            if (thisFilehash != filehash) {
                updateVerification(
                    filepath,
                    diffVal,
                    ui,
                    newCodeTaskSB,
                    filename,
                    newCodeTask,
                    prevCodeTaskSB,
                    prevCodeTask,
                    patchTaskSB,
                    patchTask,
                    newCode2TaskSB,
                    newCode2Task,
                    prevCode2TaskSB,
                    prevCode2Task,
                    patch2TaskSB,
                    patch2Task
                )
                filehash = thisFilehash
                scheduledThreadPoolExecutor.schedule(scheduledFn, 1000, TimeUnit.MILLISECONDS)
            }
        }
    }
    scheduledThreadPoolExecutor.schedule(scheduledFn, 100, TimeUnit.MILLISECONDS)
    val newValue = mainTabs + "\n" + applydiffTask.placeholder
    return newValue
}

private fun updateVerification(
    filepath: Path?,
    diffVal: String,
    ui: ApplicationInterface,
    newCodeTaskSB: StringBuilder?,
    filename: String,
    newCodeTask: SessionTask,
    prevCodeTaskSB: StringBuilder?,
    prevCodeTask: SessionTask,
    patchTaskSB: StringBuilder?,
    patchTask: SessionTask,
    newCode2TaskSB: StringBuilder?,
    newCode2Task: SessionTask,
    prevCode2TaskSB: StringBuilder?,
    prevCode2Task: SessionTask,
    patch2TaskSB: StringBuilder?,
    patch2Task: SessionTask
) {
    val prevCode = load(filepath)
    val newCode = patch(prevCode, diffVal)
    val echoDiff = try {
        DiffUtil.formatDiff(
            DiffUtil.generateDiff(
                prevCode.lines(),
                newCode.lines()
            )
        )
    } catch (e: Throwable) {
        renderMarkdown("```\n${e.stackTraceToString()}\n```", ui = ui)
    }

    newCodeTaskSB?.set(
        renderMarkdown(
            "# $filename\n\n```${filename.split('.').lastOrNull() ?: ""}\n${newCode}\n```",
            ui = ui, tabs = false
        )
    )
    newCodeTask.complete("")
    prevCodeTaskSB?.set(
        renderMarkdown(
            "# $filename\n\n```${filename.split('.').lastOrNull() ?: ""}\n${prevCode}\n```",
            ui = ui, tabs = false
        )
    )
    prevCodeTask.complete("")
    patchTaskSB?.set(renderMarkdown("# $filename\n\n```diff\n  ${echoDiff}\n```", ui = ui, tabs = false))
    patchTask.complete("")


    val newCode2 = patch(
        load(filepath).lines().reversed().joinToString("\n"),
        diffVal.lines().reversed().joinToString("\n")
    ).lines().reversed().joinToString("\n")
    val echoDiff2 = try {
        DiffUtil.formatDiff(
            DiffUtil.generateDiff(
                prevCode.lines(),
                newCode2.lines(),
            )
        )
    } catch (e: Throwable) {
        renderMarkdown("```\n${e.stackTraceToString()}\n```", ui = ui)
    }

    newCode2TaskSB?.set(
        renderMarkdown(
            "# $filename\n\n```${filename.split('.').lastOrNull() ?: ""}\n${newCode2}\n```",
            ui = ui, tabs = false
        )
    )
    newCode2Task.complete("")
    prevCode2TaskSB?.set(
        renderMarkdown(
            "# $filename\n\n```${filename.split('.').lastOrNull() ?: ""}\n${prevCode}\n```",
            ui = ui, tabs = false
        )
    )
    prevCode2Task.complete("")
    patch2TaskSB?.set(renderMarkdown("# $filename\n\n```diff\n  ${echoDiff2}\n```", ui = ui, tabs = false))
    patch2Task.complete("")
}


private fun load(
    filepath: Path?
) = try {
    if (true != filepath?.toFile()?.exists()) {
        log.warn("""File not found: $filepath""".trimMargin())
        ""
    } else {
        filepath.readText(Charsets.UTF_8)
    }
} catch (e: Throwable) {
    log.error("Error reading file: $filepath", e)
    ""
}

private fun save(
    filepath: Path?,
    code: String
) {
    try {
        filepath?.toFile()?.writeText(code, Charsets.UTF_8)
    } catch (e: Throwable) {
        log.error("Error writing file: $filepath", e)
    }
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
            null
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