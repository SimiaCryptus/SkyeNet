package com.github.simiacryptus.diff

import org.apache.commons.text.similarity.LevenshteinDistance
import org.slf4j.LoggerFactory

object IterativePatchUtil {

    enum class LineType { CONTEXT, ADD, DELETE }
    class LineRecord(
        val index: Int,
        val line: String,
        var previousLine: LineRecord? = null,
        var nextLine: LineRecord? = null,
        var matchingLine: LineRecord? = null,
        var type: LineType = LineType.CONTEXT
    ) {
        override fun toString(): String {
            val sb = StringBuilder()
            when(type) {
                LineType.CONTEXT -> sb.append(" ")
                LineType.ADD -> sb.append("+")
                LineType.DELETE -> sb.append("-")
            }
            sb.append(" ")
            sb.append(line)
            return sb.toString()
        }
    }

    fun patch(source: String, patch: String): String {
        val sourceLines = parseLines(source)
        val patchLines = parsePatchLines(patch)

        // Step 1: Link all unique lines in the source and patch that match exactly
        linkUniqueMatchingLines(sourceLines, patchLines)

        // Step 2: Link all exact matches in the source and patch which are adjacent to established links
        linkAdjacentMatchingLines(sourceLines)

        // Step 3: Establish a distance metric for matches based on Levenshtein distance and distance to established links.
        // Use this to establish the links based on a shortest-first policy and iterate until no more good matches are found.
        linkByLevenshteinDistance(sourceLines, patchLines)

        // Generate the patched text
        return generatePatchedTextUsingLinks(sourceLines, patchLines)
    }

    private fun generatePatchedTextUsingLinks(sourceLines: List<LineRecord>, patchLines: List<LineRecord>): String {
        val patchedTextBuilder = StringBuilder()
        val sourceLineBuffer = sourceLines.toMutableList()

        while (sourceLineBuffer.isNotEmpty()) {
            // Copy all lines until the next matched line
            sourceLineBuffer.takeWhile { it.matchingLine == null }.toTypedArray().forEach {
                sourceLineBuffer.remove(it)
                patchedTextBuilder.appendLine(it.line)
            }
            if(sourceLineBuffer.isEmpty()) break
            val codeLine = sourceLineBuffer.removeFirst()
            var patchLine = codeLine.matchingLine!!
            when (patchLine.type) {
                LineType.DELETE -> { /* Skip adding the line */
                }

                LineType.CONTEXT -> patchedTextBuilder.appendLine(codeLine.line)
                LineType.ADD -> {
                    throw IllegalStateException("ADD line is matched to source line")
                }
            }
            while (patchLine.nextLine?.type == LineType.ADD) {
                patchedTextBuilder.appendLine(patchLine?.nextLine?.line)
                patchLine = patchLine?.nextLine!!
            }
        }

        return patchedTextBuilder.toString().trimEnd()
    }

    private fun linkUniqueMatchingLines(sourceLines: List<LineRecord>, patchLines: List<LineRecord>) {
        val sourceLineMap = sourceLines.groupBy { it.line.trim() }
        val patchLineMap = patchLines.filter { when(it.type) {
            LineType.ADD -> false // ADD lines are not matched to source lines
            else -> true
        }}.groupBy { it.line.trim() }

        sourceLineMap.keys.intersect(patchLineMap.keys).forEach { key ->
            val sourceLine = sourceLineMap[key]?.singleOrNull()
            val patchLine = patchLineMap[key]?.singleOrNull()
            if (sourceLine != null && patchLine != null) {
                sourceLine.matchingLine = patchLine
                patchLine.matchingLine = sourceLine
            }
        }
    }

    private fun linkAdjacentMatchingLines(sourceLines: List<LineRecord>) {
        var foundMatch = true
        while (foundMatch) {
            foundMatch = false
            for (sourceLine in sourceLines) {
                val patchLine = sourceLine.matchingLine ?: continue // Skip if there's no matching line

                // Check the previous line
                if (sourceLine.previousLine != null && patchLine.previousLine != null) {
                    val sourcePrev = sourceLine.previousLine!!
                    val patchPrev = patchLine.previousLine!!
                    if (patchPrev.type != LineType.ADD && sourcePrev.line.trim() == patchPrev.line.trim() && sourcePrev.matchingLine == null && patchPrev.matchingLine == null) {
                        sourcePrev.matchingLine = patchPrev
                        patchPrev.matchingLine = sourcePrev
                        foundMatch = true
                    }
                }

                // Check the next line
                if (sourceLine.nextLine != null && patchLine.nextLine != null) {
                    val sourceNext = sourceLine.nextLine!!
                    val patchNext = patchLine.nextLine!!
                    if (patchNext.type != LineType.ADD && sourceNext.line.trim() == patchNext.line.trim() && sourceNext.matchingLine == null && patchNext.matchingLine == null) {
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

            for (patchLine in patchLines.filter { when(it.type) {
                LineType.ADD -> false // ADD lines are not matched to source lines
                else -> true
            }}) {
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

    private fun parseLines(text: String) = setLinks(text.lines().mapIndexed { index, line ->
        LineRecord(index, line)
    })

    private fun setLinks(list: List<LineRecord>): List<LineRecord> {
        for (i in 0 until list.size) {
            list[i].previousLine = if (i > 0) list[i - 1] else null
            list[i].nextLine = if (i < list.size - 1) list[i + 1] else null
        }
        return list
    }

    private fun parsePatchLines(text: String) = setLinks(text.lines().mapIndexed { index, line ->
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
    })

    private val log = LoggerFactory.getLogger(ApxPatchUtil::class.java)
}

