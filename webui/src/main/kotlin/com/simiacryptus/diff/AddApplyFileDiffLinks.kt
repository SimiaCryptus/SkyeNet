package com.simiacryptus.diff

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.skyenet.AgentPatterns
import com.simiacryptus.skyenet.AgentPatterns.displayMapInTabs
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.set
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SocketManagerBase
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import org.apache.commons.codec.digest.Md5Crypt
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.io.path.readText

private fun String.reverseLines(): String = lines().reversed().joinToString("\n")

fun SocketManagerBase.addApplyFileDiffLinks(
    root: Path,
    response: String,
    handle: (Map<Path, String>) -> Unit,
    ui: ApplicationInterface,
    api: API,
): String {
    val initiator = "(?s)```[\\w]*\n".toRegex()
    if(response.contains(initiator) && !response.split(initiator, 2)[1].contains("\n```(?![^\n])".toRegex())) {
        // Single diff block without the closing ``` due to LLM limitations... add it back
        return addApplyFileDiffLinks(
            root,
            response + "\n```",
            handle,
            ui,
            api
        )
    }
    val headerPattern = """(?<![^\n])#+\s*([^\n]+)""".toRegex() // capture filename
    val codeblockPattern = """(?s)(?<![^\n])```([^\n]*)(\n.*?\n)```""".toRegex() // capture filename
    val headers = headerPattern.findAll(response).map { it.range to it.groupValues[1] }.toList()
    val findAll = codeblockPattern.findAll(response).toList()
    val diffs: List<Pair<IntRange, String>> = findAll.filter { block ->
        val header = headers.lastOrNull { it.first.endInclusive < block.range.start }
        val filename = resolve(root, header?.second ?: "Unknown")
        when  {
            !root.toFile().resolve(filename).exists() -> false
            //block.groupValues[1] == "diff" -> true
            else -> true
        }
    }.map { it.range to it.groupValues[2] }.toList()

    val codeblocks = findAll.filter { block ->
        val header = headers.lastOrNull { it.first.endInclusive < block.range.start }
        val filename = resolve(root, header?.second ?: "Unknown")
        when  {
            root.toFile().resolve(filename).exists() -> false
            block.groupValues[1] == "diff" -> false
            else -> true
        }
    }.map { it.range to it }.toList()
    val withPatchLinks: String = diffs.fold(response) { markdown, diffBlock ->
        val header = headers.lastOrNull { it.first.endInclusive < diffBlock.first.start }
        val filename = resolve(root, header?.second ?: "Unknown")
        val diffVal = diffBlock.second
        val newValue = renderDiffBlock(root, filename, diffVal, handle, ui, api)
        val regex = "(?s)```[^\n]*\n?${Pattern.quote(diffVal)}\n?```".toRegex()
        markdown.replace(regex, newValue)
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
            codeblockRaw, displayMapInTabs(
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
                            IterativePatchUtil.generatePatch(prevCode, codeValue)
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
    if (filepath?.toFile()?.exists() == false) filepath = null // reset if file not found
    if (null != filepath) return filepath.let { root.relativize(it).toString() }.toString() // return if file found

    // if file not found, search for file in the root directory
    val files = root.toFile().recurseFiles().filter { it.name == filename.split('/', '\\').last() }
    if (files.size == 1) filepath = files.first().toPath() // if only one file found, return it
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
    ui: ApplicationInterface,
    api: API?
): String {

    val diffTask = ui.newTask(root = false)
    val prevCodeTask = ui.newTask(root = false)
    val prevCodeTaskSB = prevCodeTask.add("")
    val newCodeTask = ui.newTask(root = false)
    val newCodeTaskSB = newCodeTask.add("")
    val patchTask = ui.newTask(root = false)
    val patchTaskSB = patchTask.add("")
    val fixTask = ui.newTask(root = false)
    val prevCode2Task = ui.newTask(root = false)
    val prevCode2TaskSB = prevCode2Task.add("")
    val newCode2Task = ui.newTask(root = false)
    val newCode2TaskSB = newCode2Task.add("")
    val patch2Task = ui.newTask(root = false)
    val patch2TaskSB = patch2Task.add("")





    val filepath = path(root, filename)
    val prevCode = load(filepath)
    val relativize = try {
        root.relativize(filepath)
    } catch (e: Throwable) {
        filepath
    }
    var filehash: String = ""

    val applydiffTask = ui.newTask(false)
    lateinit var hrefLink: StringBuilder
    var isApplied = false

    var originalCode = load(filepath)
    lateinit var revert: String
    var newCode = patch(originalCode, diffVal)



    val verifyFwdTabs = if(!newCode.isValid) displayMapInTabs(
        mapOf(
            "Code" to (prevCodeTask?.placeholder ?: ""),
            "Preview" to (newCodeTask?.placeholder ?: ""),
            "Echo" to (patchTask?.placeholder ?: ""),
            "Fix" to (fixTask?.placeholder ?: ""),
        )
    ) else displayMapInTabs(
        mapOf(
            "Code" to (prevCodeTask?.placeholder ?: ""),
            "Preview" to (newCodeTask?.placeholder ?: ""),
            "Echo" to (patchTask?.placeholder ?: ""),
        )
    )
    val verifyRevTabs = displayMapInTabs(
        mapOf(
            "Code" to (prevCode2Task?.placeholder ?: ""),
            "Preview" to (newCode2Task?.placeholder ?: ""),
            "Echo" to (patch2Task?.placeholder ?: ""),
        )
    )
    val verifyTabs = displayMapInTabs(
        mapOf(
            "Forward" to verifyFwdTabs,
            "Reverse" to verifyRevTabs,
        )
    )
    val mainTabs = displayMapInTabs(
        mapOf(
            "Diff" to (diffTask?.placeholder ?: ""),
            "Verify" to verifyTabs,
        )
    )

    diffTask?.complete(renderMarkdown("```diff\n$diffVal\n```", ui = ui))

    var apply1 = hrefLink("Apply Diff", classname = "href-link cmd-button") {
        try {
            originalCode = load(filepath)
            newCode = patch(originalCode, diffVal)
            filepath?.toFile()?.writeText(newCode.newCode, Charsets.UTF_8) ?: log.warn("File not found: $filepath")
            handle(mapOf(relativize!! to newCode.newCode))
            hrefLink.set("""<div class="cmd-button">Diff Applied</div>""" + revert)
            applydiffTask.complete()
            isApplied = true
        } catch (e: Throwable) {
            hrefLink.append("""<div class="cmd-button">Error: ${e.message}</div>""")
            applydiffTask.error(null, e)
        }
    }
    if(!newCode.isValid) {
        val fixPatchLink = hrefLink("Fix Patch", classname = "href-link cmd-button") {
            try {
                val header = fixTask.header("Attempting to fix patch...")

                val patchFixer = SimpleActor(
                    prompt = """
                        |You are a helpful AI that helps people with coding.
                        |
                        |Response should use one or more code patches in diff format within ```diff code blocks.
                        |Each diff should be preceded by a header that identifies the file being modified.
                        |The diff format should use + for line additions, - for line deletions.
                        |The diff should include 2 lines of context before and after every change.
                        |
                        |Example:
                        |
                        |Here are the patches:
                        |
                        |### src/utils/exampleUtils.js
                        |```diff
                        | // Utility functions for example feature
                        | const b = 2;
                        | function exampleFunction() {
                        |-   return b + 1;
                        |+   return b + 2;
                        | }
                        |```
                        |
                        |### tests/exampleUtils.test.js
                        |```diff
                        | // Unit tests for exampleUtils
                        | const assert = require('assert');
                        | const { exampleFunction } = require('../src/utils/exampleUtils');
                        | 
                        | describe('exampleFunction', () => {
                        |-   it('should return 3', () => {
                        |+   it('should return 4', () => {
                        |     assert.equal(exampleFunction(), 3);
                        |   });
                        | });
                        |```
                        """.trimMargin(),
                    model = ChatModels.GPT4o,
                    temperature = 0.3
                )

                val echoDiff = try {
                    IterativePatchUtil.generatePatch(prevCode, newCode.newCode)
                } catch (e: Throwable) {
                    renderMarkdown("```\n${e.stackTraceToString()}\n```", ui = ui)
                }

                var answer = patchFixer.answer(
                    listOf(
                        """
                        |Code:
                        |```${filename.split('.').lastOrNull() ?: ""}
                        |$prevCode
                        |```
                        |
                        |Patch:
                        |```diff
                        |$diffVal
                        |```
                        |
                        |Effective Patch:
                        |```diff
                        |  $echoDiff
                        |```
                        |
                        |Please provide a fix for the diff above in the form of a diff patch.
                        """.trimMargin()
                    ), api as OpenAIClient
                )
                answer = ui.socketManager?.addApplyFileDiffLinks(root, answer, handle, ui, api) ?: answer
                header?.clear()
                fixTask.complete(answer)
            } catch (e: Throwable) {
                log.error("Error in fix patch", e)
            }
        }
        //apply1 += fixPatchLink
        fixTask.complete(fixPatchLink)
    }
    val apply2 = hrefLink("(Bottom to Top)", classname = "href-link cmd-button") {
        try {
            originalCode = load(filepath)
            val originalLines = originalCode.reverseLines()
            val diffLines = diffVal.reverseLines()
            val patch1 = patch(originalLines, diffLines)
            val newCode2 = patch1.newCode.reverseLines()
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
    var lastModifiedTime: Long = -1
    scheduledFn = {
        try {
            val currentModifiedTime = Files.getLastModifiedTime(filepath).toMillis()
            if (currentModifiedTime != lastModifiedTime) {
                lastModifiedTime = currentModifiedTime
                val thisFilehash = Md5Crypt.md5Crypt(filepath?.toFile()?.readText()?.toByteArray())
                if (!isApplied && thisFilehash != filehash) {
                    val newCode = patch(prevCode, diffVal)
                    val echoDiff = try {
                        IterativePatchUtil.generatePatch(prevCode, newCode.newCode)
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
                    patchTaskSB?.set(
                        renderMarkdown(
                            "# $filename\n\n```diff\n  ${echoDiff}\n```",
                            ui = ui,
                            tabs = false
                        )
                    )
                    patchTask.complete("")
                    val newCode2 = patch(
                        load(filepath).reverseLines(),
                        diffVal.reverseLines()
                    ).newCode.lines().reversed().joinToString("\n")
                    val echoDiff2 = try {
                        IterativePatchUtil.generatePatch(prevCode, newCode2)
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
                    patch2TaskSB?.set(
                        renderMarkdown(
                            "# $filename\n\n```diff\n  ${echoDiff2}\n```",
                            ui = ui,
                            tabs = false
                        )
                    )
                    patch2Task
                        .complete("")
                    filehash = thisFilehash
                }
            }
            if (!isApplied) {
                scheduledThreadPoolExecutor.schedule(scheduledFn, 1000, TimeUnit.MILLISECONDS)
            }
        } catch (e: Throwable) {
            log.error("Error in scheduled function", e)
        }
    }
    scheduledThreadPoolExecutor.schedule(scheduledFn, 1000, TimeUnit.MILLISECONDS)
    val newValue = mainTabs + "\n" + applydiffTask.placeholder
    return newValue
}

private val patch = { code: String, diff: String ->
    val isCurlyBalanced = FileValidationUtils.isCurlyBalanced(code)
    val isSquareBalanced = FileValidationUtils.isSquareBalanced(code)
    val isParenthesisBalanced = FileValidationUtils.isParenthesisBalanced(code)
    val isQuoteBalanced = FileValidationUtils.isQuoteBalanced(code)
    val isSingleQuoteBalanced = FileValidationUtils.isSingleQuoteBalanced(code)
    var newCode = IterativePatchUtil.patch(code, diff)
    newCode = newCode.replace("\r", "")
    val isCurlyBalancedNew = FileValidationUtils.isCurlyBalanced(newCode)
    val isSquareBalancedNew = FileValidationUtils.isSquareBalanced(newCode)
    val isParenthesisBalancedNew = FileValidationUtils.isParenthesisBalanced(newCode)
    val isQuoteBalancedNew = FileValidationUtils.isQuoteBalanced(newCode)
    val isSingleQuoteBalancedNew = FileValidationUtils.isSingleQuoteBalanced(newCode)
    val isError = ((isCurlyBalanced && !isCurlyBalancedNew) ||
            (isSquareBalanced && !isSquareBalancedNew) ||
            (isParenthesisBalanced && !isParenthesisBalancedNew) ||
            (isQuoteBalanced && !isQuoteBalancedNew) ||
            (isSingleQuoteBalanced && !isSingleQuoteBalancedNew))
    PatchResult(newCode, !isError)
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
            root.resolve(filename).toFile().exists() -> root.resolve(filename)

            /* filename is absolute */
            filename.startsWith("/") -> {
                val resolve = File(filename)
                if (resolve.exists()) resolve.toPath() else findFile(root, filename.removePrefix("/"))
            }
            /* windows absolute */
            filename.indexOf(":\\") == 1 -> {
                val resolve = File(filename)
                if (resolve.exists()) resolve.toPath() else findFile(
                    root,
                    filename.removePrefix(filename.substring(0, 2))
                )
            }

            null != root.parent && root != root.parent -> findFile(root.parent, filename)
            else -> null
        }
    } catch (e: Throwable) {
        log.error("Error finding file: $filename", e)
        null
    }
}

@Suppress("unused")
fun applyFileDiffs(
    root: Path,
    response: String,
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
    diffs.forEach { diffBlock ->
        val header = headers.lastOrNull { it.first.endInclusive < diffBlock.first.start }
        val filename = resolve(root, header?.second ?: "Unknown")
        val diffVal = diffBlock.second
        val filepath = root.resolve(filename)
        try {
            val originalCode = filepath.readText(Charsets.UTF_8)
            val newCode = patch(originalCode, diffVal)
            filepath?.toFile()?.writeText(newCode.newCode, Charsets.UTF_8) ?: log.warn("File not found: $filepath")
        } catch (e: Throwable) {
            log.warn("Error", e)
        }
    }
    codeblocks.forEach { codeBlock ->
        val header = headers.lastOrNull { it.first.endInclusive < codeBlock.first.start }
        val filename = resolve(root, header?.second ?: "Unknown")
        val filepath: Path? = root.resolve(filename)
        val codeValue = codeBlock.second.groupValues[2]
        lateinit var hrefLink: StringBuilder
        try {
            try {
                filepath?.toFile()?.writeText(codeValue, Charsets.UTF_8)
            } catch (e: Throwable) {
                log.error("Error writing file: $filepath", e)
            }
            hrefLink.set("""<div class="cmd-button">Saved ${filename}</div>""")
        } catch (e: Throwable) {
            log.error("Error", e)
        }
    }
    return response
}


private val log = org.slf4j.LoggerFactory.getLogger(PatchUtil::class.java)