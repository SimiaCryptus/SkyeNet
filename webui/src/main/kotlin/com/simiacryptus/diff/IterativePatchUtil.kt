@file:Suppress("LoggingSimilarMessage")

package com.simiacryptus.diff

import org.apache.commons.text.similarity.LevenshteinDistance
import org.slf4j.LoggerFactory
import kotlin.math.max
import kotlin.math.min

object IterativePatchUtil {
    enum class LineType { CONTEXT, ADD, DELETE }

    // Tracks the nesting depth of different bracket types
    data class LineMetrics(
        var parenthesesDepth: Int = 0,
        var squareBracketsDepth: Int = 0,
        var curlyBracesDepth: Int = 0
    )

    // Represents a single line in the source or patch text
    data class LineRecord(
        val index: Int,
        val line: String?,
        var previousLine: LineRecord? = null,
        var nextLine: LineRecord? = null,
        var matchingLine: LineRecord? = null,
        var type: LineType = LineType.CONTEXT,
        var metrics: LineMetrics = LineMetrics()
    ) {
        override fun toString(): String {
            val sb = StringBuilder()
            sb.append("${index.toString().padStart(5, ' ')}: ")
            when (type) {
                LineType.CONTEXT -> sb.append(" ")
                LineType.ADD -> sb.append("+")
                LineType.DELETE -> sb.append("-")
            }
            sb.append(" ")
            sb.append(line)
            sb.append(" [({:${metrics.parenthesesDepth}, [:${metrics.squareBracketsDepth}, {:${metrics.curlyBracesDepth}]")
            return sb.toString()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as LineRecord

            if (index != other.index) return false
            if (line != other.line) return false
            if (type != other.type) return false
            if (metrics != other.metrics) return false

            return true
        }

        override fun hashCode(): Int {
            var result = index
            result = 31 * result + (line?.hashCode() ?: 0)
            result = 31 * result + type.hashCode()
            result = 31 * result + metrics.hashCode()
            return result
        }


    }

    /**
     * Generates an optimal patch by comparing two code files.
     * @param oldCode The original code.
     * @param newCode The new code.
     * @return The generated patch as a string.
     */
    fun generatePatch(oldCode: String, newCode: String): String {
        log.info("Starting patch generation process")
        val sourceLines = parseLines(oldCode)
        val newLines = parseLines(newCode)
        link(sourceLines, newLines)
        log.debug("Parsed and linked source lines: ${sourceLines.size}, new lines: ${newLines.size}")
        val diff1 = diffLines(sourceLines, newLines)
        val diff = truncateContext(diff1).toMutableList()
        fixPatchLineOrder(diff)
        annihilateNoopLinePairs(diff)
        log.debug("Generated diff with ${diff.size} lines after processing")
        val patch = StringBuilder()
        // Generate the patch text
        diff.forEach { line ->
            when (line.type) {
                LineType.CONTEXT -> patch.append("  ${line.line}\n")
                LineType.ADD -> patch.append("+ ${line.line}\n")
                LineType.DELETE -> patch.append("- ${line.line}\n")
            }
        }
        log.info("Patch generation completed")
        return patch.toString().trimEnd()
    }

    /**
     * Applies a patch to the given source text.
     * @param source The original text.
     * @param patch The patch to apply.
     * @return The text after the patch has been applied.
     */
    fun applyPatch(source: String, patch: String): String {
        log.info("Starting patch application process")
        // Parse the source and patch texts into lists of line records
        val sourceLines = parseLines(source)
        var patchLines = parsePatchLines(patch)
        log.debug("Parsed source lines: ${sourceLines.size}, initial patch lines: ${patchLines.size}")
        link(sourceLines, patchLines)

        // Filter out empty lines in the patch
        patchLines = patchLines.filter { it.line?.let { normalizeLine(it).isEmpty() } == false }
        log.debug("Filtered patch lines: ${patchLines.size}")
        log.info("Generating patched text")

        val result = generatePatchedText(sourceLines, patchLines)
        val generatePatchedTextUsingLinks = result.joinToString("\n").trim()
        log.info("Patch application completed")

        return generatePatchedTextUsingLinks
    }

    private fun annihilateNoopLinePairs(diff: MutableList<LineRecord>) {
        log.debug("Starting annihilation of no-op line pairs")
        val toRemove = mutableListOf<Pair<Int, Int>>()
        var i = 0
        while (i < diff.size) {
            if (diff[i].type == LineType.DELETE) {
                var j = i + 1
                while (j < diff.size && diff[j].type != LineType.CONTEXT) {
                    if (diff[j].type == LineType.ADD &&
                        normalizeLine(diff[i].line ?: "") == normalizeLine(diff[j].line ?: "")
                    ) {
                        toRemove.add(Pair(i, j))
                        break
                    }
                    j++
                }
            }
            i++
        }
        // Remove the pairs in reverse order to maintain correct indices
        toRemove.sortedByDescending { it.first }.forEach { (deleteIndex, addIndex) ->
            diff.removeAt(addIndex)
            diff.removeAt(deleteIndex)
        }
        log.debug("Removed ${toRemove.size} no-op line pairs")
    }

    private fun diffLines(
        sourceLines: List<LineRecord>,
        newLines: List<LineRecord>
    ): MutableList<LineRecord> {
        val diff = mutableListOf<LineRecord>()
        log.debug("Starting diff generation")
        var sourceIndex = 0
        var newIndex = 0
        // Generate raw patch without limited context windows
        while (sourceIndex < sourceLines.size || newIndex < newLines.size) {
            when {
                sourceIndex >= sourceLines.size -> {
                    // Add remaining new lines
                    diff.add(newLines[newIndex].copy(type = LineType.ADD))
                    newIndex++
                }

                newIndex >= newLines.size -> {
                    // Delete remaining source lines
                    diff.add(sourceLines[sourceIndex].copy(type = LineType.DELETE))
                    sourceIndex++
                }

                sourceLines[sourceIndex].matchingLine == newLines[newIndex] &&
                        normalizeLine(sourceLines[sourceIndex].line ?: "") == normalizeLine(
                    newLines[newIndex].line ?: ""
                ) -> {
                    // Lines match, add as context
                    diff.add(sourceLines[sourceIndex].copy(type = LineType.CONTEXT))
                    sourceIndex++
                    newIndex++
                }

                sourceLines[sourceIndex].matchingLine == null ||
                        normalizeLine(sourceLines[sourceIndex].line ?: "") != normalizeLine(
                    newLines[newIndex].line ?: ""
                ) -> {
                    // Source line has no match, it's a deletion
                    diff.add(sourceLines[sourceIndex].copy(type = LineType.DELETE))
                    sourceIndex++
                }

                else -> {
                    // New line has no match in source, it's an addition
                    diff.add(newLines[newIndex].copy(type = LineType.ADD))
                    newIndex++
                }
            }
        }
        log.debug("Generated diff with ${diff.size} lines")
        return diff
    }

    private fun truncateContext(diff: MutableList<LineRecord>): MutableList<LineRecord> {
        val contextSize = 3 // Number of context lines before and after changes
        log.debug("Truncating context with size $contextSize")
        val truncatedDiff = mutableListOf<LineRecord>()
        var inChange = false
        val contextBuffer = mutableListOf<LineRecord>()
        var lastChangeIndex = -1
        for (i in diff.indices) {
            val line = diff[i]
            when {
                line.type != LineType.CONTEXT -> {
                    if (!inChange) {
                        // Start of a change, add buffered context
                        truncatedDiff.addAll(contextBuffer.takeLast(contextSize))
                        contextBuffer.clear()
                    }
                    truncatedDiff.add(line)
                    inChange = true
                    lastChangeIndex = i
                }

                inChange -> {
                    contextBuffer.add(line)
                    if (contextBuffer.size == contextSize) {
                        // End of a change, add buffered context
                        truncatedDiff.addAll(contextBuffer)
                        contextBuffer.clear()
                        inChange = false
                    }
                }

                else -> {
                    contextBuffer.add(line)
                    if (contextBuffer.size > contextSize) {
                        contextBuffer.removeAt(0)
                    }
                }
            }
        }
        // Add trailing context after the last change
        if (lastChangeIndex != -1) {
            val trailingContext = diff.subList(lastChangeIndex + 1, min(diff.size, lastChangeIndex + 1 + contextSize))
            truncatedDiff.addAll(trailingContext)
        }
        log.debug("Truncated diff size: ${truncatedDiff.size}")
        return truncatedDiff
    }

    /**
     * Normalizes a line by removing all whitespace.
     * @param line The line to normalize.
     * @return The normalized line.
     */
    private fun normalizeLine(line: String): String {
        return line.replace("\\s".toRegex(), "")
    }

    private fun link(
        sourceLines: List<LineRecord>,
        patchLines: List<LineRecord>
    ) {
        // Step 1: Link all unique lines in the source and patch that match exactly
        log.info("Step 1: Linking unique matching lines")
        linkUniqueMatchingLines(sourceLines, patchLines)

        // Step 2: Link all exact matches in the source and patch which are adjacent to established links
        log.info("Step 2: Linking adjacent matching lines")
        linkAdjacentMatchingLines(sourceLines)
        log.info("Step 3: Performing subsequence linking")

        subsequenceLinking(sourceLines, patchLines)
    }

    private fun subsequenceLinking(
        sourceLines: List<LineRecord>,
        patchLines: List<LineRecord>,
        depth: Int = 0
    ) {
        log.debug("Subsequence linking at depth $depth")
        if (depth > 10 || sourceLines.isEmpty() || patchLines.isEmpty()) {
            return // Base case: prevent excessive recursion
        }
        val sourceSegment = sourceLines.filter { it.matchingLine == null }
        val patchSegment = patchLines.filter { it.matchingLine == null }
        if (sourceSegment.isNotEmpty() && patchSegment.isNotEmpty()) {
            var matchedLines = linkUniqueMatchingLines(sourceSegment, patchSegment)
            matchedLines += linkAdjacentMatchingLines(sourceSegment)
            if (matchedLines == 0) {
                matchedLines += matchFirstBrackets(sourceSegment, patchSegment)
            }
            if (matchedLines > 0) {
                subsequenceLinking(sourceSegment, patchSegment, depth + 1)
            }
            log.debug("Matched $matchedLines lines in subsequence linking at depth $depth")

        }
    }

    private fun generatePatchedText(
        sourceLines: List<LineRecord>,
        patchLines: List<LineRecord>,
    ): List<String> {
        log.debug("Starting to generate patched text")
        val patchedText: MutableList<String> = mutableListOf()
        val usedPatchLines = mutableSetOf<LineRecord>()
        var sourceIndex = 0
        var lastMatchedPatchIndex = -1
        while (sourceIndex < sourceLines.size) {
            val codeLine = sourceLines[sourceIndex]
            when {
                codeLine.matchingLine?.type == LineType.DELETE -> {
                    log.debug("Deleted line: {}", codeLine)
                    sourceIndex++
                }

                codeLine.matchingLine != null -> {
                    val patchLine = codeLine.matchingLine!!
                    var patchIndex = patchLines.indexOf(patchLine)
                    log.debug("Patching line: {} <-> {}", codeLine, patchLine)
                    // Add context lines between last match and current match
                    if (lastMatchedPatchIndex != -1) {
                        for (i in lastMatchedPatchIndex + 1 until patchIndex) {
                            val contextLine = patchLines[i]
                            if (contextLine.type == LineType.CONTEXT && !usedPatchLines.contains(contextLine)) {
                                patchedText.add(contextLine.line ?: "")
                                usedPatchLines.add(contextLine)
                            }
                        }
                    }

                    // Check preceding patch lines for insertions
                    while (patchIndex > 0) {
                        val prevPatchLine = patchLines[--patchIndex]
                        if (prevPatchLine.type == LineType.ADD && !usedPatchLines.contains(prevPatchLine)) {
                            log.debug("Added unmatched patch line: {}", prevPatchLine)
                            patchedText.add(prevPatchLine.line ?: "")
                            usedPatchLines.add(prevPatchLine)
                        } else {
                            break
                        }
                    }
                    // Add the patched line
                    patchedText.add(patchLine.line ?: "")

                    // Check following patch lines for insertions
                    patchIndex = patchLines.indexOf(patchLine)
                    while (patchIndex < patchLines.size - 1) {
                        val nextPatchLine = patchLines[++patchIndex]
                        if (nextPatchLine.type == LineType.ADD && !usedPatchLines.contains(nextPatchLine)) {
                            log.debug("Added unmatched patch line: {}", nextPatchLine)
                            patchedText.add(nextPatchLine.line ?: "")
                            usedPatchLines.add(nextPatchLine)
                        } else {
                            break
                        }
                    }

                    usedPatchLines.add(patchLine)
                    lastMatchedPatchIndex = patchIndex
                    sourceIndex++
                }

                else -> {
                    // Check if this line is a context line in the patch
                    val contextPatchLine = patchLines.find { it.type == LineType.CONTEXT && it.line == codeLine.line }
                    if (contextPatchLine != null) {
                        log.debug("Added context line: {}", codeLine)
                        patchedText.add(contextPatchLine.line ?: "")
                        usedPatchLines.add(contextPatchLine)
                    } else {
                        log.debug("Added unmatched source line: {}", codeLine)
                        patchedText.add(codeLine.line ?: "")
                    }
                    sourceIndex++
                }

            }
        }
        // Add remaining context lines after the last match
        if (lastMatchedPatchIndex != -1) {
            for (i in lastMatchedPatchIndex + 1 until patchLines.size) {
                val contextLine = patchLines[i]
                if (contextLine.type == LineType.CONTEXT && !usedPatchLines.contains(contextLine)) {
                    patchedText.add(contextLine.line ?: "")
                    usedPatchLines.add(contextLine)
                }
            }
        }
        // Add any remaining unused ADD lines from the patch
        patchLines.filter { it.type == LineType.ADD && !usedPatchLines.contains(it) }.forEach { line ->
            log.debug("Added remaining patch line: {}", line)
            patchedText.add(line.line ?: "")
        }
        log.debug("Generated patched text with ${patchedText.size} lines")
        return patchedText
    }

    private fun matchFirstBrackets(sourceLines: List<LineRecord>, patchLines: List<LineRecord>): Int {
        log.debug("Starting to match first brackets")
        log.debug("Starting to link unique matching lines")
        // Group source lines by their normalized content
        val sourceLineMap = sourceLines.filter {
            it.line?.lineMetrics() != LineMetrics()
        }.groupBy { normalizeLine(it.line!!) }
        // Group patch lines by their normalized content, excluding ADD lines
        val patchLineMap = patchLines.filter {
            it.line?.lineMetrics() != LineMetrics()
        }.filter {
            when (it.type) {
                LineType.ADD -> false // ADD lines are not matched to source lines
                else -> true
            }
        }.groupBy { normalizeLine(it.line!!) }
        log.debug("Created source and patch line maps")

        // Find intersecting keys (matching lines) and link them
        val matched = sourceLineMap.keys.intersect(patchLineMap.keys)
        matched.forEach { key ->
            val sourceGroup = sourceLineMap[key]!!
            val patchGroup = patchLineMap[key]!!
            for (i in 0 until min(sourceGroup.size, patchGroup.size)) {
                sourceGroup[i].matchingLine = patchGroup[i]
                patchGroup[i].matchingLine = sourceGroup[i]
                log.debug("Linked matching lines: Source[${sourceGroup[i].index}]: ${sourceGroup[i].line} <-> Patch[${patchGroup[i].index}]: ${patchGroup[i].line}")
            }
        }
        val matchedCount = matched.sumOf { sourceLineMap[it]!!.size }
        log.debug("Finished matching first brackets. Matched $matchedCount lines")
        return matched.sumOf { sourceLineMap[it]!!.size }
    }

    /**
     * Links lines between the source and the patch that are unique and match exactly.
     * @param sourceLines The source lines.
     * @param patchLines The patch lines.
     */
    private fun linkUniqueMatchingLines(sourceLines: List<LineRecord>, patchLines: List<LineRecord>): Int {
        log.debug("Starting to link unique matching lines. Source lines: ${sourceLines.size}, Patch lines: ${patchLines.size}")
        // Group source lines by their normalized content
        val sourceLineMap = sourceLines.groupBy { normalizeLine(it.line!!) }
        // Group patch lines by their normalized content, excluding ADD lines
        val patchLineMap = patchLines.filter {
            when (it.type) {
                LineType.ADD -> false // ADD lines are not matched to source lines
                else -> true
            }
        }.groupBy { normalizeLine(it.line!!) }
        log.debug("Created source and patch line maps")

        // Find intersecting keys (matching lines) and link them
        val matched = sourceLineMap.keys.intersect(patchLineMap.keys).filter {
            sourceLineMap[it]?.size == patchLineMap[it]?.size
        }
        matched.forEach { key ->
            val sourceGroup = sourceLineMap[key]!!
            val patchGroup = patchLineMap[key]!!
            for (i in sourceGroup.indices) {
                sourceGroup[i].matchingLine = patchGroup[i]
                patchGroup[i].matchingLine = sourceGroup[i]
                log.debug("Linked unique matching lines: Source[${sourceGroup[i].index}]: ${sourceGroup[i].line} <-> Patch[${patchGroup[i].index}]: ${patchGroup[i].line}")
            }
        }
        val matchedCount = matched.sumOf { sourceLineMap[it]!!.size }
        log.debug("Finished linking unique matching lines. Matched $matchedCount lines")
        return matched.sumOf { sourceLineMap[it]!!.size }
    }

    /**
     * Links lines that are adjacent to already linked lines and match exactly.
     * @param sourceLines The source lines with some established links.
     */
    private fun linkAdjacentMatchingLines(sourceLines: List<LineRecord>): Int {
        log.debug("Starting to link adjacent matching lines. Source lines: ${sourceLines.size}")
        var foundMatch = true
        var matchedLines = 0
        val levenshteinDistance = LevenshteinDistance()
        // Continue linking until no more matches are found
        while (foundMatch) {
            log.debug("Starting new iteration to find adjacent matches")
            foundMatch = false
            for (sourceLine in sourceLines) {
                val patchLine = sourceLine.matchingLine ?: continue // Skip if there's no matching line

                // Check the previous line for a potential match
                if (sourceLine.previousLine != null && patchLine.previousLine != null) {
                    var sourcePrev = sourceLine.previousLine!!
                    var patchPrev = patchLine.previousLine!!
                    while (patchPrev.previousLine != null && (
                                patchPrev.type == LineType.ADD // Skip ADD lines in the patch when looking for matches
                                        || normalizeLine(patchPrev.line ?: "").isEmpty() // Skip empty lines
                                )
                    ) {
                        patchPrev = patchPrev.previousLine!!
                    }
                    while (sourcePrev.previousLine != null && (
                                normalizeLine(sourcePrev.line ?: "").isEmpty() // Skip empty lines
                                )
                    ) {
                        sourcePrev = sourcePrev.previousLine!!
                    }
                    if (sourcePrev.matchingLine == null && patchPrev.matchingLine == null) { // Skip if there's already a match
                        var isMatch = normalizeLine(sourcePrev.line!!) == normalizeLine(patchPrev.line!!)
                        val length = max(sourcePrev.line!!.length, patchPrev.line!!.length)
                        if (!isMatch && length > 5) { // Check if the lines are similar using Levenshtein distance
                            val distance = levenshteinDistance.apply(sourcePrev.line, patchPrev.line)
                            log.debug("Levenshtein distance: $distance")
                            isMatch = distance <= (length / 3)
                        }
                        if (isMatch) { // Check if the lines match exactly
                            sourcePrev.matchingLine = patchPrev
                            patchPrev.matchingLine = sourcePrev
                            foundMatch = true
                            matchedLines++
                            log.debug("Linked adjacent previous lines: Source[${sourcePrev.index}]: ${sourcePrev.line} <-> Patch[${patchPrev.index}]: ${patchPrev.line}")
                        }
                    }
                }

                // Check the next line for a potential match
                if (sourceLine.nextLine != null && patchLine.nextLine != null) {
                    var sourceNext = sourceLine.nextLine!!
                    var patchNext = patchLine.nextLine!!
                    // Skip ADD lines in the patch when looking for matches
                    while (patchNext.nextLine != null && (
                                patchNext.type == LineType.ADD
                                        || normalizeLine(patchNext.line ?: "").isEmpty()
                                )
                    ) {
                        patchNext = patchNext.nextLine!!
                    }
                    while (sourceNext.nextLine != null && (
                                normalizeLine(sourceNext.line ?: "").isEmpty()
                                )
                    ) {
                        sourceNext = sourceNext.nextLine!!
                    }
                    if (sourceNext.matchingLine == null && patchNext.matchingLine == null) {
                        if (normalizeLine(sourceNext.line!!) == normalizeLine(patchNext.line!!)) {
                            sourceNext.matchingLine = patchNext
                            patchNext.matchingLine = sourceNext
                            foundMatch = true
                            matchedLines++
                            log.debug("Linked adjacent next lines: Source[${sourceNext.index}]: ${sourceNext.line} <-> Patch[${patchNext.index}]: ${patchNext.line}")
                        }
                    }
                }
            }
        }
        log.debug("Finished linking adjacent matching lines. Matched $matchedLines lines")
        return matchedLines
    }

    /**
     * @param text The text to parse.
     * @return The list of line records.
     */
    private fun parseLines(text: String): List<LineRecord> {
        log.debug("Starting to parse lines")
        // Create LineRecords for each line and set links between them
        val lines = setLinks(text.lines().mapIndexed { index, line -> LineRecord(index, line) })
        // Calculate bracket metrics for each line
        calculateLineMetrics(lines)
        log.debug("Finished parsing ${lines.size} lines")
        return lines
    }

    /**
     * Sets the previous and next line links for a list of line records.
     * @return The list with links set.
     */
    private fun setLinks(list: List<LineRecord>): List<LineRecord> {
        log.debug("Starting to set links for ${list.size} lines")
        for (i in list.indices) {
            list[i].previousLine = if (i > 0) list[i - 1] else null
            list[i].nextLine = if (i < list.size - 1) list[i + 1] else null
        }
        log.debug("Finished setting links for ${list.size} lines")
        return list
    }

    /**
     * Parses the patch text into a list of line records, identifying the type of each line (ADD, DELETE, CONTEXT).
     * @param text The patch text to parse.
     * @return The list of line records with types set.
     */
    private fun parsePatchLines(text: String): List<LineRecord> {
        log.debug("Starting to parse patch lines")
        val patchLines = setLinks(text.lines().mapIndexed { index, line ->
            LineRecord(
                index = index,
                line = line.let {
                    when {
                        it.trimStart().startsWith("+++") -> null
                        it.trimStart().startsWith("---") -> null
                        it.trimStart().startsWith("@@") -> null
                        it.trimStart().startsWith("+") -> it.trimStart().substring(1)
                        it.trimStart().startsWith("-") -> it.trimStart().substring(1)
                        else -> it
                    }
                },
                type = when {
                    line.startsWith("+") -> LineType.ADD
                    line.startsWith("-") -> LineType.DELETE
                    else -> LineType.CONTEXT
                }
            )
        }.filter { it.line != null }).toMutableList()

        fixPatchLineOrder(patchLines)

        calculateLineMetrics(patchLines)
        log.debug("Finished parsing ${patchLines.size} patch lines")
        return patchLines
    }

    private fun fixPatchLineOrder(patchLines: MutableList<LineRecord>) {
        log.debug("Starting to fix patch line order")
        // Fixup: Iterate over the patch lines and look for adjacent ADD and DELETE lines; the DELETE should come first... if needed, swap them
        var swapped: Boolean
        do {
            swapped = false
            for (i in 0 until patchLines.size - 1) {
                if (patchLines[i].type == LineType.ADD && patchLines[i + 1].type == LineType.DELETE) {
                    swapped = true
                    val deleteLine = patchLines[i]
                    val addLine = patchLines[i + 1]
                    // Swap records and update pointers
                    deleteLine.nextLine = addLine.nextLine
                    addLine.previousLine = deleteLine.previousLine
                    deleteLine.previousLine = addLine
                    addLine.nextLine = deleteLine
                    patchLines[i] = addLine
                    patchLines[i + 1] = deleteLine
                }
            }
        } while (swapped)
        log.debug("Finished fixing patch line order")
    }

    /**
     * Calculates the metrics for each line, including bracket nesting depth.
     * @param lines The list of line records to process.
     */
    private fun calculateLineMetrics(lines: List<LineRecord>) {
        log.debug("Starting to calculate line metrics for ${lines.size} lines")
        var parenthesesDepth = 0
        var squareBracketsDepth = 0
        var curlyBracesDepth = 0

        lines.forEach { lineRecord ->
            lineRecord.line?.forEach { char ->
                when (char) {
                    '(' -> parenthesesDepth++
                    ')' -> parenthesesDepth = maxOf(0, parenthesesDepth - 1)
                    '[' -> squareBracketsDepth++
                    ']' -> squareBracketsDepth = maxOf(0, squareBracketsDepth - 1)
                    '{' -> curlyBracesDepth++
                    '}' -> curlyBracesDepth = maxOf(0, curlyBracesDepth - 1)
                }
            }
            lineRecord.metrics = LineMetrics(
                parenthesesDepth = parenthesesDepth,
                squareBracketsDepth = squareBracketsDepth,
                curlyBracesDepth = curlyBracesDepth
            )
        }
        log.debug("Finished calculating line metrics")
    }

    fun String.lineMetrics(): LineMetrics {
        var parenthesesDepth = 0
        var squareBracketsDepth = 0
        var curlyBracesDepth = 0

        this.forEach { char ->
            when (char) {
                '(' -> parenthesesDepth++
                ')' -> parenthesesDepth = maxOf(0, parenthesesDepth - 1)
                '[' -> squareBracketsDepth++
                ']' -> squareBracketsDepth = maxOf(0, squareBracketsDepth - 1)
                '{' -> curlyBracesDepth++
                '}' -> curlyBracesDepth = maxOf(0, curlyBracesDepth - 1)
            }
        }
        return LineMetrics(
            parenthesesDepth = parenthesesDepth,
            squareBracketsDepth = squareBracketsDepth,
            curlyBracesDepth = curlyBracesDepth
        )
    }

    private val log = LoggerFactory.getLogger(IterativePatchUtil::class.java)

}