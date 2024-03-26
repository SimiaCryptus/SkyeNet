package com.github.simiacryptus.aicoder.util

import com.simiacryptus.skyenet.AgentPatterns.displayMapInTabs
import com.simiacryptus.skyenet.core.actors.CodingActor.Companion.indent
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.session.SocketManagerBase
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import org.apache.commons.text.StringEscapeUtils.escapeHtml4
import org.apache.commons.text.similarity.LevenshteinDistance
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

object SimpleDiffUtil {


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
          SimpleDiffUtil.patch(lines, patch)
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
        val newReversedCode = SimpleDiffUtil.patch(reversedCode, reversedDiff)
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
    """(?s)(?<![^\n])#+\s*(?:[^\n]+[:\-]\s+)?([^\n]+)\n```[^\n]*\n(.*?)```""".toRegex() // capture filename
  val matches = diffPattern.findAll(response).distinct()
  val withLinks = matches.fold(response) { markdown, diffBlock ->
    val filename = diffBlock.groupValues[1]
//    val language = diffBlock.groupValues[2]
    val codeValue = diffBlock.groupValues[2]
    val hrefLink = hrefLink("Save File") {
      try {
        handle(filename, codeValue)
        task.complete("""<div class="user-message">Saved ${filename}</div>""")
      } catch (e: Throwable) {
        task.error(null, e)
      }
    }
    markdown.replace(codeValue + "```", codeValue?.let { escapeHtml4(it).indent("  ") } + "```\n" + hrefLink)
  }
  return withLinks
}

fun SocketManagerBase.addApplyDiffLinks2(
  code: Map<String, String>,
  response: String,
  handle: (Map<String, String>) -> Unit,
  task: SessionTask,
  ui: ApplicationInterface?,
): String {
  val diffPattern = """(?s)(?<![^\n])#+\s*([^\n]+)(?:[^`]+`?)*\n(```diff\n.*?\n```)""".toRegex() // capture filename
  val matches = diffPattern.findAll(response).toList()
  val withLinks = matches.fold(response) { markdown, diffBlock ->
    val filename = diffBlock.groupValues[1]
    val diffVal = diffBlock.groupValues[2]
    val prevCode = code[filename] ?: ""
    val newCode = SimpleDiffUtil.patch(prevCode, diffVal)
    val echoDiff = DiffMatchPatch.patch_toText(DiffMatchPatch.Companion.patch_make(prevCode, newCode))
    val hrefLink = hrefLink("Apply Diff") {
      try {
        val newCode = code.map { (file, prevCode) ->
          file to when (filename) {
            file -> SimpleDiffUtil.patch(prevCode, diffVal)
            else -> prevCode
          }
        }.toMap()
        handle(newCode)
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
            SimpleDiffUtil.patch(prevCode, reversedDiff).lines().reversed().joinToString("\n")
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
        "Old Code" to (prevCodeTask?.placeholder ?: ""),
        "New Code" to (newCodeTask?.placeholder ?: ""),
        "Test Patch" to (patchTask?.placeholder ?: ""),
      )
    )
    SocketManagerBase.scheduledThreadPoolExecutor.schedule({
      diffTask?.add(renderMarkdown(/*escapeHtml4*/(diffVal)))
      newCodeTask?.add(renderMarkdown("# $filename\n\n```${filename.split('.').lastOrNull() ?: ""}\n${newCode}\n```"))
      prevCodeTask?.add(renderMarkdown("# $filename\n\n```${filename.split('.').lastOrNull() ?: ""}\n${prevCode}\n```"))
      patchTask?.add(renderMarkdown("# $filename\n\n```diff\n  ${echoDiff?.let { /*escapeHtml4*/URLDecoder.decode(it, Charsets.UTF_8)/*.indent("  ")*/ }}\n```"))
    }, 100, TimeUnit.MILLISECONDS)
    markdown.replace(
      diffVal, inTabs + "\n" + hrefLink + "\n" + reverseHrefLink
    )
  }
  return withLinks
}