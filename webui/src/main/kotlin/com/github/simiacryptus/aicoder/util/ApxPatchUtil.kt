package com.github.simiacryptus.aicoder.util

import com.simiacryptus.skyenet.AgentPatterns.displayMapInTabs
import com.simiacryptus.skyenet.core.actors.CodingActor.Companion.indent
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.session.SocketManagerBase
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import org.apache.commons.text.similarity.LevenshteinDistance
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.readText

object ApxPatchUtil {


  fun patch(source: String, patch: String): String {
    val sourceLines = source.lines()
    val patchLines = patch.lines()

    // This will hold the final result
    val result = mutableListOf<String>()

    // This will keep track of the current line in the source file
    var sourceIndex = 0

    // Process each line in the patch
    for (patchLine in patchLines.map { it.trim() }) {
      when {
        // If the line starts with "---" or "+++", it's a file indicator line, skip it
        patchLine.startsWith("---") || patchLine.startsWith("+++") -> continue

        // If the line starts with "@@", it's a hunk header
        patchLine.startsWith("@@") -> continue

        // If the line starts with "-", it's a deletion, skip the corresponding source line but otherwise treat it as a context line
        patchLine.startsWith("-") -> {
          sourceIndex = onDelete(patchLine, sourceIndex, sourceLines, result)
        }

        // If the line starts with "+", it's an addition, add it to the result
        patchLine.startsWith("+") -> {
          result.add(patchLine.substring(1))
        }

        // \d+\: ___ is a line number, strip it
        patchLine.matches(Regex("\\d+:.*")) -> {
          sourceIndex = onContextLine(patchLine.substringAfter(":"), sourceIndex, sourceLines, result)
        }

        // it's a context line, advance the source cursor
        else -> {
          sourceIndex = onContextLine(patchLine, sourceIndex, sourceLines, result)
        }
      }
    }

    // Append any remaining lines from the source file
    while (sourceIndex < sourceLines.size) {
      result.add(sourceLines[sourceIndex])
      sourceIndex++
    }

    return result.joinToString("\n")
  }

  private fun onDelete(
    patchLine: String,
    sourceIndex: Int,
    sourceLines: List<String>,
    result: MutableList<String>
  ): Int {
    var sourceIndex1 = sourceIndex
    val delLine = patchLine.substring(1)
    val sourceIndexSearch = lookAheadFor(sourceIndex1, sourceLines, delLine)
    if (sourceIndexSearch > 0 && sourceIndexSearch + 1 < sourceLines.size) {
      val contextChunk = sourceLines.subList(sourceIndex1, sourceIndexSearch)
      result.addAll(contextChunk)
      sourceIndex1 = sourceIndexSearch + 1
    } else {
      println("Deletion line not found in source file: $delLine")
      // Ignore
    }
    return sourceIndex1
  }

  private fun onContextLine(
    patchLine: String,
    sourceIndex: Int,
    sourceLines: List<String>,
    result: MutableList<String>
  ): Int {
    var sourceIndex1 = sourceIndex
    val sourceIndexSearch = lookAheadFor(sourceIndex1, sourceLines, patchLine)
    if (sourceIndexSearch > 0 && sourceIndexSearch + 1 < sourceLines.size) {
      val contextChunk = sourceLines.subList(sourceIndex1, sourceIndexSearch + 1)
      result.addAll(contextChunk)
      sourceIndex1 = sourceIndexSearch + 1
    } else {
      println("Context line not found in source file: $patchLine")
      // Ignore
    }
    return sourceIndex1
  }

  private fun lookAheadFor(
    sourceIndex: Int,
    sourceLines: List<String>,
    patchLine: String
  ): Int {
    var sourceIndexSearch = sourceIndex
    while (sourceIndexSearch < sourceLines.size) {
      if (lineMatches(patchLine, sourceLines[sourceIndexSearch++])) return sourceIndexSearch - 1
    }
    return -1
  }

  private fun lineMatches(
    a: String,
    b: String,
    factor: Double = 0.3
  ): Boolean {
    val threshold = (Math.max(a.trim().length, b.trim().length) * factor).toInt()
    val levenshteinDistance = LevenshteinDistance(5)
    val dist = levenshteinDistance.apply(a.trim(), b.trim())
    return if (dist >= 0) {
      dist <= threshold
    } else {
      false
    }
  }
}

fun SocketManagerBase.addApplyDiffLinks(
  code: String,
  response: String,
  fullPatch: MutableList<String> = mutableListOf(),
  handle: (String) -> Unit,
  task: SessionTask,
  ui: ApplicationInterface? = null,
): String {
  val diffPattern = """(?s)(?<![^\n])```diff\n(.*?)\n```""".toRegex()
  val matches = diffPattern.findAll(response).distinct()
  val withLinks = matches.fold(response) { markdown, diffBlock ->
    val diffVal = diffBlock.value
    val hrefLink = hrefLink("Apply Diff") {
      try {
        if (fullPatch.contains(diffVal)) return@hrefLink
        fullPatch.add(diffVal)
        val newCode = fullPatch.fold(code) { lines, patch ->
          ApxPatchUtil.patch(lines, patch)
        }
        handle(newCode)
        task.complete("""<div class="user-message">Diff Applied</div>""")
      } catch (e: Throwable) {
        task.error(ui, e)
      }
    }
    val reverseHrefLink = hrefLink("(Bottom to Top)") {
      try {
        if (fullPatch.contains(diffVal)) return@hrefLink
        fullPatch.add(diffVal)
        val reversedCode = code.lines().reversed().joinToString("\n")
        val reversedDiff = diffVal.lines().reversed().joinToString("\n")
        val newReversedCode = ApxPatchUtil.patch(reversedCode, reversedDiff)
        val newCode = newReversedCode.lines().reversed().joinToString("\n")
        handle(newCode)
        task.complete("""<div class="user-message">Diff Applied (Bottom to Top)</div>""")
      } catch (e: Throwable) {
        task.error(ui, e)
      }
    }
    markdown.replace(diffVal, diffVal + "\n" + hrefLink + "\n" + reverseHrefLink)
  }
  return withLinks
}

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
    markdown.replace(codeValue + "```", codeValue?.let { /*escapeHtml4*/(it).indent("  ") } + "```\n" + hrefLink)
  }
  return withLinks
}

private val log = LoggerFactory.getLogger(ApxPatchUtil::class.java)

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
        if (resolve.exists()) resolve.toPath() else findFile(root, filename.removePrefix(filename.substring(0, 2)))
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

fun SocketManagerBase.addApplyDiffLinks2(
  root: Path,
  code: Map<String, String>,
  response: String,
  handle: (Map<String, String>) -> Unit,
  task: SessionTask,
  ui: ApplicationInterface,
): String {
  val headerPattern = """(?s)(?<![^\n])#+\s*([^\n]+)""".toRegex() // capture filename
  val diffPattern = """(?s)(?<![^\n])(```diff\n.*?\n```)""".toRegex() // capture filename
  val codeblockPattern = """(?s)(?<![^\n])```([^\n])(\n.*?\n)```""".toRegex() // capture filename
  val headers = headerPattern.findAll(response).map { it.range to it.groupValues[1] }.toList()
  val diffs: List<Pair<IntRange, String>> = diffPattern.findAll(response).map { it.range to it.groupValues[1] }.toList()
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
    val newValue = renderDiffBlock(root, filename, code, diffVal, handle, task, ui)
    markdown.replace(diffVal, newValue)
  }
  val withSaveLinks = codeblocks.fold(withPatchLinks) { markdown, codeBlock ->
    val header = headers.lastOrNull { it.first.endInclusive < codeBlock.first.start }
    val filename = header?.second ?: "Unknown"
    val filepath = path(root, filename)
    val prevCode = load(filepath, root, code)
    val codeLang = codeBlock.second.groupValues[1]
    val codeValue = codeBlock.second.groupValues[2]
    val hrefLink = hrefLink("Save File") {
      try {
        handle(
          mapOf(
            filename to codeValue
          )
        )
        task.complete("""<div class="user-message">Saved ${filename}</div>""")
      } catch (e: Throwable) {
        task.error(null, e)
      }
    }
    val codeblockRaw = """
      ```${codeLang}
      ${codeValue}
      ```
      """.trimIndent()
    markdown.replace(
      codeblockRaw, displayMapInTabs(
        mapOf(
          "New" to renderMarkdown(codeblockRaw),
          "Old" to renderMarkdown("""
          |```${codeLang}
          |${prevCode}
          |```
          """.trimMargin()),
          "Patch" to renderMarkdown("""
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
          """.trimMargin()),
        )
      ) + "\n" + hrefLink
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
  task: SessionTask,
  ui: ApplicationInterface
): String {
  val filepath = path(root, filename)
  val prevCode = load(filepath, root, code)
  val newCode = ApxPatchUtil.patch(prevCode, diffVal)
  val echoDiff = try {
    DiffUtil.formatDiff(
      DiffUtil.generateDiff(
        prevCode.lines(),
        newCode.lines()
      )
    )
  } catch (e: Throwable) {
    renderMarkdown("```\n${e.stackTraceToString()}\n```")
  }

  val hrefLink = hrefLink("Apply Diff") {
    try {
      val relativize = try {
        root.relativize(filepath)
      } catch (e: Throwable) {
        filepath
      }
      handle(
        mapOf(
          relativize.toString() to ApxPatchUtil.patch(
            prevCode,
            diffVal
          )
        )
      )
      task.complete("""<div class="user-message">Diff Applied</div>""")
    } catch (e: Throwable) {
      task.error(null, e)
    }
  }
  val reverseHrefLink = hrefLink("(Bottom to Top)") {
    try {
      val reversedCodeMap = code.mapValues { (_, v) -> v.lines().reversed().joinToString("\n") }
      val reversedDiff = diffVal.lines().reversed().joinToString("\n")
      val newReversedCodeMap = reversedCodeMap.mapValues { (file, prevCode) ->
        if (filename == file) {
          ApxPatchUtil.patch(prevCode, reversedDiff).lines().reversed().joinToString("\n")
        } else prevCode
      }
      handle(newReversedCodeMap)
      task.complete("""<div class="user-message">Diff Applied (Bottom to Top)</div>""")
    } catch (e: Throwable) {
      task.error(null, e)
    }
  }
  val diffTask = ui?.newTask()
  val prevCodeTask = ui?.newTask()
  val newCodeTask = ui?.newTask()
  val patchTask = ui?.newTask()
  val inTabs = displayMapInTabs(
    mapOf(
      "Diff" to (diffTask?.placeholder ?: ""),
      "Code" to (prevCodeTask?.placeholder ?: ""),
      "Preview" to (newCodeTask?.placeholder ?: ""),
      "Echo" to (patchTask?.placeholder ?: ""),
    )
  )
  SocketManagerBase.scheduledThreadPoolExecutor.schedule({
    diffTask?.add(renderMarkdown(/*escapeHtml4*/(diffVal)))
    newCodeTask?.add(renderMarkdown("# $filename\n\n```${filename.split('.').lastOrNull() ?: ""}\n${newCode}\n```"))
    prevCodeTask?.add(renderMarkdown("# $filename\n\n```${filename.split('.').lastOrNull() ?: ""}\n${prevCode}\n```"))
    patchTask?.add(renderMarkdown("# $filename\n\n```diff\n  ${echoDiff}\n```"))
  }, 100, TimeUnit.MILLISECONDS)
  val newValue = inTabs + "\n" + hrefLink + "\n" + reverseHrefLink
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