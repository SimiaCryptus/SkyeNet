package com.github.simiacryptus.aicoder.util

import org.apache.commons.text.similarity.LevenshteinDistance
import org.slf4j.LoggerFactory

object IterativePatchUtil {

  enum class LineType { CONTEXT, ADD, DELETE }
  class LineRecord(
    val index: Int,
    val line: String,
    val previousLine: LineRecord? = null,
    val nextLine: LineRecord? = null,
    var matchingLine: LineRecord? = null,
    var type: LineType = LineType.CONTEXT
  )

  fun patch(source: String, patch: String): String {
    val sourceLines = parseLines(source)
    val patchLines = parsePatchLines(patch)

    // Step 1: Link all unique lines in the source and patch that match exactly
    linkUniqueMatchingLines(sourceLines, patchLines)

    // Step 2: Link all exact matches in the source and patch which are adjacent to established links
    linkAdjacentMatchingLines(sourceLines, patchLines)

    // Step 3: Establish a distance metric for matches based on Levenshtein distance and distance to established links.
    // Use this to establish the links based on a shortest-first policy and iterate until no more good matches are found.
    linkByLevenshteinDistance(sourceLines, patchLines)

    // Generate the patched text
    return generatePatchedText(sourceLines, patchLines)
  }

  private fun generatePatchedText(sourceLines: List<LineRecord>, patchLines: List<LineRecord>): String {
    val patchedTextBuilder = StringBuilder()

    // Add unmatched code lines at the beginning
    sourceLines
      .takeWhile { it.matchingLine == null }
      .forEach { patchedTextBuilder.appendln(it.line) }

    // Iterate through patch lines and apply changes to the source
    patchLines.forEach { patchLine ->
      when (patchLine.type) {
        LineType.ADD -> patchedTextBuilder.appendln(patchLine.line)
        LineType.DELETE -> {
          // Skip adding the line to the patched text
        }

        LineType.CONTEXT -> {
          // For context lines, we need to find the corresponding line in the source
          // If there's a matching line, we add the source line to maintain original formatting
          // If not, we add the patch line (it could be a case where context lines don't match exactly due to trimming)
          val sourceLine = sourceLines.find { it.index == patchLine.index && it.matchingLine == patchLine }
          if (sourceLine != null) {
            patchedTextBuilder.appendln(sourceLine.line)
          } else {
            patchedTextBuilder.appendln(patchLine.line)
          }
        }
      }
    }

    // Add unmatched code lines at the end
    sourceLines.reversed()
      .takeWhile { it.matchingLine == null }
      .reversed()
      .forEach { patchedTextBuilder.appendln(it.line) }

    return patchedTextBuilder.toString().trimEnd()
  }

  private fun linkUniqueMatchingLines(sourceLines: List<LineRecord>, patchLines: List<LineRecord>) {
    val sourceLineMap = sourceLines.groupBy { it.line.trim() }
    val patchLineMap = patchLines.groupBy { it.line.trim() }

    sourceLineMap.keys.intersect(patchLineMap.keys).forEach { key ->
      val sourceLine = sourceLineMap[key]?.singleOrNull()
      val patchLine = patchLineMap[key]?.singleOrNull()
      if (sourceLine != null && patchLine != null) {
        sourceLine.matchingLine = patchLine
        patchLine.matchingLine = sourceLine
      }
    }
  }

  private fun linkAdjacentMatchingLines(sourceLines: List<LineRecord>, patchLines: List<LineRecord>) {
    var foundMatch = true
    while (foundMatch) {
      foundMatch = false
      for (sourceLine in sourceLines) {
        val patchLine = sourceLine.matchingLine ?: continue // Skip if there's no matching line

        // Check the previous line
        if (sourceLine.previousLine != null && patchLine.previousLine != null) {
          val sourcePrev = sourceLine.previousLine
          val patchPrev = patchLine.previousLine
          if (sourcePrev.line.trim() == patchPrev.line.trim() && sourcePrev.matchingLine == null && patchPrev.matchingLine == null) {
            sourcePrev.matchingLine = patchPrev
            patchPrev.matchingLine = sourcePrev
            foundMatch = true
          }
        }

        // Check the next line
        if (sourceLine.nextLine != null && patchLine.nextLine != null) {
          val sourceNext = sourceLine.nextLine
          val patchNext = patchLine.nextLine
          if (sourceNext.line.trim() == patchNext.line.trim() && sourceNext.matchingLine == null && patchNext.matchingLine == null) {
            sourceNext.matchingLine = patchNext
            patchNext.matchingLine = sourceNext
            foundMatch = true
          }
        }
      }
    }
  }

  private fun linkByLevenshteinDistance(sourceLines: List<LineRecord>, patchLines: List<LineRecord>) {
    val levenshteinDistance = LevenshteinDistance()
    val maxDistance = 5 // Define a maximum acceptable distance. Adjust as needed.

    // Iterate over source lines to find potential matches in the patch lines
    for (sourceLine in sourceLines) {
      if (sourceLine.matchingLine != null) continue // Skip lines that already have matches

      var bestMatch: LineRecord? = null
      var bestDistance = Int.MAX_VALUE
      var bestCombinedDistance = Int.MAX_VALUE

      for (patchLine in patchLines) {
        if (patchLine.matchingLine != null) continue // Skip lines that already have matches

        val distance = levenshteinDistance.apply(sourceLine.line.trim(), patchLine.line.trim())
        if (distance <= maxDistance) {
          // Calculate combined distance, factoring in proximity to established links
          val combinedDistance = distance + calculateProximityDistance(sourceLine, patchLine)

          if (combinedDistance < bestCombinedDistance) {
            bestMatch = patchLine
            bestDistance = distance
            bestCombinedDistance = combinedDistance
          }
        }
      }

      if (bestMatch != null) {
        // Establish the best match
        sourceLine.matchingLine = bestMatch
        bestMatch.matchingLine = sourceLine
      }
    }
  }

  private fun calculateProximityDistance(sourceLine: LineRecord, patchLine: LineRecord): Int {
    // Implement logic to calculate proximity distance based on the distance to the nearest established link
    // This is a simplified example. You may need a more sophisticated approach based on your specific requirements.
    var distance = 0
    if (sourceLine.previousLine?.matchingLine != null || patchLine.previousLine?.matchingLine != null) {
      distance += 1
    }
    if (sourceLine.nextLine?.matchingLine != null || patchLine.nextLine?.matchingLine != null) {
      distance += 1
    }
    return distance
  }

  private fun parseLines(text: String): List<LineRecord> {
    return text.lines().mapIndexed { index, line ->
      LineRecord(index, line)
    }
  }

  private fun parsePatchLines(text: String): List<LineRecord> {
    return text.lines().mapIndexed { index, line ->
      LineRecord(
        index = index, line = line.let {
          when {
            it.trimStart().startsWith("+") -> it.trimStart().substring(1)
            it.trimStart().startsWith("-") -> it.trimStart().substring(1)
            else -> it
          }
        }, type = when {
          line.startsWith("+") -> LineType.ADD
          line.startsWith("-") -> LineType.DELETE
          else -> LineType.CONTEXT
        }
      )
    }
  }

  private val log = LoggerFactory.getLogger(ApxPatchUtil::class.java)
}

