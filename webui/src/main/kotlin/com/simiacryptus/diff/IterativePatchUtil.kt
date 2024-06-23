package com.simiacryptus.diff

import org.apache.commons.text.similarity.LevenshteinDistance
import kotlin.math.floor

object IterativePatchUtil {

    enum class LineType { CONTEXT, ADD, DELETE }
    class LineRecord(
        val index: Int,
        val line: String?,
        var previousLine: LineRecord? = null,
        var nextLine: LineRecord? = null,
        var matchingLine: LineRecord? = null,
        var type: LineType = LineType.CONTEXT
    ) {
        override fun toString(): String {
            val sb = StringBuilder()
            when (type) {
                LineType.CONTEXT -> sb.append(" ")
                LineType.ADD -> sb.append("+")
                LineType.DELETE -> sb.append("-")
            }
            sb.append(" ")
            sb.append(line)
            return sb.toString()
        }
    }

    /**
     * Normalizes a line by removing all whitespace.
     * @param line The line to normalize.
     * @return The normalized line.
     */
    private fun normalizeLine(line: String): String {
        return line.replace("\\s".toRegex(), "")
    }

    /**
     * Applies a patch to the given source text.
     * @param source The original text.
     * @param patch The patch to apply.
     * @return The text after the patch has been applied.
     */
    fun patch(source: String, patch: String): String {
        // Parse the source and patch texts into lists of line records
        val sourceLines = parseLines(source)
        val patchLines = parsePatchLines(patch)

        // Step 1: Link all unique lines in the source and patch that match exactly
        linkUniqueMatchingLines(sourceLines, patchLines)

        // Step 2: Link all exact matches in the source and patch which are adjacent to established links
        linkAdjacentMatchingLines(sourceLines)

        // Step 3: Establish a distance metric for matches based on Levenshtein distance and distance to established links.
        // Use this to establish the links based on a shortest-first policy and iterate until no more good matches are found.
        //linkByLevenshteinDistance(sourceLines, patchLines)

        // Generate the patched text using the established links
        return generatePatchedTextUsingLinks(sourceLines, patchLines)
    }

    /**
     * Generates the final patched text using the links established between the source and patch lines.
     * @param sourceLines The source lines with established links.
     * @param patchLines The patch lines with established links.
     * @return The final patched text.
     */
    private fun generatePatchedTextUsingLinks(sourceLines: List<LineRecord>, patchLines: List<LineRecord>): String {
        val patchedTextBuilder = StringBuilder()
        val sourceLineBuffer = sourceLines.toMutableList()

        // Add any leading lines from the source that are not in the patch
        while (sourceLineBuffer.firstOrNull()?.matchingLine == null) {
            patchedTextBuilder.appendLine(sourceLineBuffer.removeFirst().line)
        }

        // Add any leading 'add' lines from the patch
        val patchLines = patchLines.toMutableList()
        while (patchLines.firstOrNull()?.type == LineType.ADD) {
            patchedTextBuilder.appendLine(patchLines.removeFirst().line)
        }

        // Process the rest of the lines
        while (sourceLineBuffer.isNotEmpty()) {
            // Copy all lines until the next matched line
            val codeLine = sourceLineBuffer.removeFirst()
            when {
                codeLine.matchingLine == null -> {
                    // If the line is not matched and is adjacent to a non-matched line, add it as a context line
                    if (codeLine.nextLine?.matchingLine == null || codeLine.previousLine?.matchingLine == null) {
                        patchedTextBuilder.appendLine(codeLine.line)
                    }
                }

                codeLine.matchingLine!!.type == LineType.DELETE -> null // Skip adding the line
                codeLine.matchingLine!!.type == LineType.CONTEXT -> patchedTextBuilder.appendLine(codeLine.line)
                codeLine.matchingLine!!.type == LineType.ADD -> throw IllegalStateException("ADD line is matched to source line")
            }

            // Add lines marked as ADD in the patch following the current matched line
            var nextPatchLine = codeLine.matchingLine?.nextLine
            while (nextPatchLine != null && nextPatchLine.matchingLine == null) {
                when (nextPatchLine.type) {
                    LineType.ADD -> patchedTextBuilder.appendLine(nextPatchLine.line)
                    LineType.CONTEXT -> patchedTextBuilder.appendLine(nextPatchLine.line)
                    LineType.DELETE -> null // Skip adding the line
                }
                nextPatchLine = nextPatchLine.nextLine
            }
        }
        return patchedTextBuilder.toString().trimEnd()
    }

     // ... rest of the file remains unchanged
    /**
     * Links lines between the source and the patch that are unique and match exactly.
     * @param sourceLines The source lines.
     * @param patchLines The patch lines.
     */
    private fun linkUniqueMatchingLines(sourceLines: List<LineRecord>, patchLines: List<LineRecord>) {
        val sourceLineMap = sourceLines.groupBy { normalizeLine(it.line!!) }
        val patchLineMap = patchLines.filter {
            when (it.type) {
                LineType.ADD -> false // ADD lines are not matched to source lines
                else -> true
            }
        }.groupBy { normalizeLine(it.line!!) }

        sourceLineMap.keys.intersect(patchLineMap.keys).forEach { key ->
            val sourceLine = sourceLineMap[key]?.singleOrNull()
            val patchLine = patchLineMap[key]?.singleOrNull()
            if (sourceLine != null && patchLine != null) {
                sourceLine.matchingLine = patchLine
                patchLine.matchingLine = sourceLine
            }
        }
    }

    /**
     * Links lines that are adjacent to already linked lines and match exactly.
     * @param sourceLines The source lines with some established links.
     */
    private fun linkAdjacentMatchingLines(sourceLines: List<LineRecord>) {
        var foundMatch = true
        while (foundMatch) {
            foundMatch = false
            for (sourceLine in sourceLines) {
                val patchLine = sourceLine.matchingLine ?: continue // Skip if there's no matching line

                // Check the previous line for a potential match
                if (sourceLine.previousLine != null && patchLine.previousLine != null) {
                    val sourcePrev = sourceLine.previousLine!!
                    var patchPrev = patchLine.previousLine!!
                    while (patchPrev.type == LineType.ADD && patchPrev.previousLine != null) {
                        patchPrev = patchPrev.previousLine!!
                    }
                    if (sourcePrev.matchingLine == null && patchPrev.matchingLine == null) { // Skip if there's already a match
                        if (normalizeLine(sourcePrev.line!!) == normalizeLine(patchPrev.line!!)) { // Check if the lines match exactly
                            sourcePrev.matchingLine = patchPrev
                            patchPrev.matchingLine = sourcePrev
                            foundMatch = true
                        }
                    }
                }

                // Check the next line for a potential match
                if (sourceLine.nextLine != null && patchLine.nextLine != null) {
                    val sourceNext = sourceLine.nextLine!!
                    var patchNext = patchLine.nextLine!!
                    while (patchNext.type == LineType.ADD && patchNext.nextLine != null) {
                        patchNext = patchNext.nextLine!!
                    }
                    if (sourceNext.matchingLine == null && patchNext.matchingLine == null) {
                        if (normalizeLine(sourceNext.line!!) == normalizeLine(patchNext.line!!)) {
                            sourceNext.matchingLine = patchNext
                            patchNext.matchingLine = sourceNext
                            foundMatch = true
                        }
                    }
                }
            }
        }
    }

    /**
     * Establishes links between source and patch lines based on the Levenshtein distance and proximity to already established links.
     * @param sourceLines The source lines.
     * @param patchLines The patch lines.
     */
    private fun linkByLevenshteinDistance(sourceLines: List<LineRecord>, patchLines: List<LineRecord>) {
        val levenshteinDistance = LevenshteinDistance()
        val maxProximity = (sourceLines.size + patchLines.size) / 10 // Increase max distance to allow more flexibility

        // Iterate over source lines to find potential matches in the patch lines
        for (sourceLine in sourceLines) {
            if (sourceLine.matchingLine != null) continue // Skip lines that already have matches

            var bestMatch: LineRecord? = null
            var bestDistance = Int.MAX_VALUE
            var bestProximity = Int.MAX_VALUE

            for (patchLine in patchLines.filter {
                when (it.type) {
                    LineType.ADD -> false // ADD lines are not matched to source lines
                    else -> true
                }
            }) {
                if (patchLine.matchingLine != null) continue // Skip lines that already have matches
                val maxDistance = minOf(bestDistance, floor(patchLine.line!!.length.toDouble() / 2).toInt())
                // Calculate the Levenshtein distance between unmatched source and patch lines
                val distance =
                    levenshteinDistance.apply(normalizeLine(sourceLine.line!!), normalizeLine(patchLine.line!!))
                if (distance <= maxDistance) {
                    // Consider proximity to established links as a secondary factor
                    val proximity = calculateProximityDistance(sourceLine, patchLine)
                    if(proximity > maxProximity) continue
                    if (distance < bestDistance || (distance == bestDistance && proximity < bestProximity)) {
                        bestMatch = patchLine
                        bestDistance = distance
                        bestProximity = proximity
                    }
                }

                // Establish the best match found, if any
                if (bestMatch != null) {
                    sourceLine.matchingLine = bestMatch
                    bestMatch.matchingLine = sourceLine
                }
            }
        }
    }

    /**
     * Calculates the proximity distance between a source line and a patch line based on their distance to the nearest established link.
     * @param sourceLine The source line.
     * @param patchLine The patch line.
     * @return The proximity distance.
     */
    private fun calculateProximityDistance(sourceLine: LineRecord, patchLine: LineRecord): Int {
        // Find the nearest established link in both directions for source and patch lines
        var sourceDistancePrev = 0
        var sourceDistanceNext = 0
        var patchDistancePrev = 0
        var patchDistanceNext = 0

        var currentSourceLine = sourceLine.previousLine
        while (currentSourceLine != null) {
            if (currentSourceLine.matchingLine != null) break
            sourceDistancePrev++
            currentSourceLine = currentSourceLine.previousLine
        }

        currentSourceLine = sourceLine.nextLine
        while (currentSourceLine != null) {
            if (currentSourceLine.matchingLine != null) break
            sourceDistanceNext++
            currentSourceLine = currentSourceLine.nextLine
        }

        var currentPatchLine = patchLine.previousLine
        while (currentPatchLine != null) {
            if (currentPatchLine.matchingLine != null) break
            patchDistancePrev++
            currentPatchLine = currentPatchLine.previousLine
        }

        currentPatchLine = patchLine.nextLine
        while (currentPatchLine != null) {
            if (currentPatchLine.matchingLine != null) break
            patchDistanceNext++
            currentPatchLine = currentPatchLine.nextLine
        }

        // Calculate the total proximity distance as the sum of minimum distances in each direction
        return minOf(sourceDistancePrev, patchDistancePrev) + minOf(sourceDistanceNext, patchDistanceNext)
    }

    /**
     * Parses the given text into a list of line records.
     * @param text The text to parse.
     * @return The list of line records.
     */
    private fun parseLines(text: String) = setLinks(text.lines().mapIndexed { index, line ->
        LineRecord(index, line)
    })

    /**
     * Sets the previous and next line links for a list of line records.
     * @param list The list of line records.
     * @return The list with links set.
     */
    private fun setLinks(list: List<LineRecord>): List<LineRecord> {
        for (i in 0 until list.size) {
            list[i].previousLine = if (i > 0) list[i - 1] else null
            list[i].nextLine = if (i < list.size - 1) list[i + 1] else null
        }
        return list
    }

    /**
     * Parses the patch text into a list of line records, identifying the type of each line (ADD, DELETE, CONTEXT).
     * @param text The patch text to parse.
     * @return The list of line records with types set.
     */
    private fun parsePatchLines(text: String) = setLinks(text.lines().mapIndexed { index, line ->
        LineRecord(
            index = index, line = line.let {
                when {
                    it.trimStart().startsWith("+++") -> null
                    it.trimStart().startsWith("---") -> null
                    it.trimStart().startsWith("@@") -> null
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
    }.filter { it.line != null })

}