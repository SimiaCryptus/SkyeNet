package com.simiacryptus.diff

import org.apache.commons.text.similarity.LevenshteinDistance
import org.slf4j.LoggerFactory
import kotlin.math.max
import kotlin.math.min

object IterativePatchUtil {
    private val log = LoggerFactory.getLogger(IterativePatchUtil::class.java)

    enum class LineType { CONTEXT, ADD, DELETE }

    // Tracks the nesting depth of different bracket types
    data class LineMetrics(
        var parenthesesDepth: Int = 0,
        var squareBracketsDepth: Int = 0,
        var curlyBracesDepth: Int = 0
    )

    // Represents a single line in the source or patch text
    class LineRecord(
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
     * Generates an optimal patch by comparing two code files.
     * @param oldCode The original code.
     * @param newCode The new code.
     * @return The generated patch as a string.
     */
    fun generatePatch(oldCode: String, newCode: String): String {
        log.info("Generating patch")
        val oldLines = oldCode.lines()
        val newLines = newCode.lines()
        val lcs = longestCommonSubsequence(oldLines, newLines)
        val diff = mutableListOf<String>()
        var oldIndex = 0
        var newIndex = 0
        var lcsIndex = 0
        while (oldIndex < oldLines.size || newIndex < newLines.size) {
            // Output a hunk header
            val oldStart = oldIndex + 1
            val newStart = newIndex + 1
            val hunkDiff = mutableListOf<String>()
            // Process lines until we've covered 3 lines of context after changes
            var contextLines = 0
            while (oldIndex < oldLines.size || newIndex < newLines.size) {
                if (lcsIndex < lcs.size && oldLines[oldIndex] == lcs[lcsIndex] && newLines[newIndex] == lcs[lcsIndex]) {
                    // Matching line (context)
                    hunkDiff.add(" ${oldLines[oldIndex]}")
                    oldIndex++
                    newIndex++
                    lcsIndex++
                    if (contextLines > 0) contextLines++
                    if (contextLines >= 3) break
                } else if (newIndex < newLines.size && (lcsIndex >= lcs.size || newLines[newIndex] != lcs[lcsIndex])) {
                    // Added line
                    hunkDiff.add("+${newLines[newIndex]}")
                    newIndex++
                    contextLines = 0
                } else if (oldIndex < oldLines.size && (lcsIndex >= lcs.size || oldLines[oldIndex] != lcs[lcsIndex])) {
                    // Removed line
                    hunkDiff.add("-${oldLines[oldIndex]}")
                    oldIndex++
                    contextLines = 0
                }
            }
            if (hunkDiff.isNotEmpty()) {
                diff.add("@@ -$oldStart,${oldIndex - oldStart + 1} +$newStart,${newIndex - newStart + 1} @@")
                diff.addAll(hunkDiff)
            }
        }
        return diff.joinToString("\n")
    }
    private fun longestCommonSubsequence(a: List<String>, b: List<String>): List<String> {
        val lengths = Array(a.size + 1) { IntArray(b.size + 1) }
        for (i in a.indices.reversed()) {
            for (j in b.indices.reversed()) {
                if (a[i] == b[j])
                    lengths[i][j] = lengths[i + 1][j + 1] + 1
                else
                    lengths[i][j] = maxOf(lengths[i + 1][j], lengths[i][j + 1])
            }
        }
        val result = mutableListOf<String>()
        var i = 0
        var j = 0
        while (i < a.size && j < b.size) {
            if (a[i] == b[j]) {
                result.add(a[i])
                i++
                j++
            } else if (lengths[i + 1][j] >= lengths[i][j + 1]) {
                i++
            } else {
                j++
            }
        }
        return result
    }

    private fun createHunk(
        oldLines: List<String>,
        newLines: List<String>,
        oldStart: Int,
        newStart: Int
    ): Triple<List<String>, Int, Int> {
        val hunk = mutableListOf<String>()
        var oldIndex = oldStart
        var newIndex = newStart
        var oldCount = 0
        var newCount = 0
        val context = 3
        val hunkStart = oldIndex
        val hunkNewStart = newIndex
        // Find the end of the differences
        while (oldIndex < oldLines.size && newIndex < newLines.size) {
            if (oldLines[oldIndex] != newLines[newIndex]) {
                // Move oldIndex and newIndex to the end of the difference
                while (oldIndex < oldLines.size && (newIndex >= newLines.size || oldLines[oldIndex] != newLines[newIndex])) {
                    oldIndex++
                    oldCount++
                }
                while (newIndex < newLines.size && (oldIndex >= oldLines.size || oldLines[oldIndex] != newLines[newIndex])) {
                    newIndex++
                    newCount++
                }
                // Add context lines after the change
                var contextAfter = 0
                while (contextAfter < context && oldIndex < oldLines.size && newIndex < newLines.size && oldLines[oldIndex] == newLines[newIndex]) {
                    oldIndex++
                    newIndex++
                    oldCount++
                    newCount++
                    contextAfter++
                }
                break
            }
            oldIndex++
            newIndex++
            oldCount++
            newCount++
        }
        // Adjust start indices to include context before the change
        oldIndex = maxOf(hunkStart, oldIndex - oldCount - context)
        newIndex = maxOf(hunkNewStart, newIndex - newCount - context)
        oldCount = 0
        newCount = 0
        // Add the hunk header
        hunk.add("@@ -${oldIndex + 1},${oldLines.size - oldIndex} +${newIndex + 1},${newLines.size - newIndex} @@")
        // Add the changes
        while (oldIndex < oldLines.size || newIndex < newLines.size) {
            if (oldIndex < oldLines.size && (newIndex >= newLines.size || oldLines[oldIndex] != newLines[newIndex])) {
                hunk.add("-${oldLines[oldIndex]}")
                oldIndex++
                oldCount++
            } else if (newIndex < newLines.size && (oldIndex >= oldLines.size || oldLines[oldIndex] != newLines[newIndex])) {
                hunk.add("+${newLines[newIndex]}")
                newIndex++
                newCount++
            } else if (oldIndex < oldLines.size && newIndex < newLines.size) {
                hunk.add(" ${oldLines[oldIndex]}")
                oldIndex++; newIndex++; oldCount++; newCount++
            }
        }
        return Triple(hunk, oldCount, newCount)
    }

    /**
     * Creates a patch string from the comparison of old and new code lines.
     * @param oldLines The old code lines with established links.
     * @param newLines The new code lines with established links.
     * @return The generated patch as a string.
     */
    private fun createPatchFromComparison(oldLines: List<LineRecord>, newLines: List<LineRecord>): String {
        log.debug("Creating patch from comparison")
        val patchBuilder = StringBuilder()
        var oldIndex = 0
        var newIndex = 0
        var contextLines = 3 // Number of context lines to include
        var addedLines = 0
        var removedLines = 0
        var lastPrintedOldIndex = -1
        var lastPrintedNewIndex = -1

        // Iterate through both old and new lines to create the patch
        while (oldIndex < oldLines.size || newIndex < newLines.size) {


            if (oldIndex < oldLines.size && (newIndex >= newLines.size || oldLines[oldIndex].matchingLine == null)) {
                // Line removed
                if (addedLines + removedLines == 0) printContext(
                    patchBuilder,
                    oldLines,
                    newLines,
                    oldIndex,
                    newIndex,
                    contextLines
                )
                patchBuilder.append("- ${oldLines[oldIndex].line}\n")
                oldIndex++
                removedLines++
                lastPrintedOldIndex = oldIndex - 1
            } else if (newIndex < newLines.size && (oldIndex >= oldLines.size || newLines[newIndex].matchingLine == null)) {
                // Line added
                if (addedLines + removedLines == 0) printContext(
                    patchBuilder,
                    oldLines,
                    newLines,
                    oldIndex,
                    newIndex,
                    contextLines
                )
                patchBuilder.append("+ ${newLines[newIndex].line}\n")
                newIndex++
                addedLines++
                lastPrintedNewIndex = newIndex - 1
            } else {
                // Matching line (context)
                if (addedLines + removedLines > 0) {
                    printContext(patchBuilder, oldLines, newLines, oldIndex, newIndex, contextLines)
                    addedLines = 0
                    removedLines = 0
                }
                oldIndex++
                newIndex++
            }
        }
        // Print final context if needed
        if (addedLines + removedLines > 0) {
            printContext(patchBuilder, oldLines, newLines, oldIndex, newIndex, contextLines)
        }

        log.debug("Patch creation completed")
        return patchBuilder.toString()
    }

    private fun printContext(
        patchBuilder: StringBuilder, oldLines: List<LineRecord>, newLines: List<LineRecord>,
        oldIndex: Int, newIndex: Int, contextLines: Int
    ) {
        val startOld = maxOf(0, oldIndex - contextLines)
        val endOld = minOf(oldLines.size, oldIndex + contextLines)
        val startNew = maxOf(0, newIndex - contextLines)
        val endNew = minOf(newLines.size, newIndex + contextLines)
        patchBuilder.append("@@ -${startOld + 1},${endOld - startOld} +${startNew + 1},${endNew - startNew} @@\n")
        for (i in startOld until endOld) {
            if (i < oldIndex - contextLines || i >= oldIndex + contextLines) continue
            patchBuilder.append(" ${oldLines[i].line}\n")
        }
    }

    /**
     * Applies a patch to the given source text.
     * @param source The original text.
     * @param patch The patch to apply.
     * @return The text after the patch has been applied.
     */
    fun patch(source: String, patch: String): String {
        // Compare source and patch to establish links between matching lines
        var (sourceLines, patchLines) = compare(source, patch)

        patchLines =
            patchLines.filter { it.line?.let { normalizeLine(it).isEmpty() } == false } // Filter out empty lines in the patch

        // Generate the patched text using the established links
        log.info("Generating patched text using established links")
        val generatePatchedTextUsingLinks = generatePatchedTextUsingLinks(sourceLines, patchLines).trim()

        log.info("Patch process completed")
        return generatePatchedTextUsingLinks
    }

    private fun compare(
        source: String,
        patch: String
    ): Pair<List<LineRecord>, List<LineRecord>> {
        log.info("Starting patch process")
        // Parse the source and patch texts into lists of line records
        val sourceLines = parseLines(source)
        val patchLines = parsePatchLines(patch)
        log.debug("Parsed source lines: ${sourceLines.size}, patch lines: ${patchLines.size}")

        // Step 1: Link all unique lines in the source and patch that match exactly
        log.info("Step 1: Linking unique matching lines")
        linkUniqueMatchingLines(sourceLines, patchLines)

        // Step 2: Link all exact matches in the source and patch which are adjacent to established links
        log.info("Step 2: Linking adjacent matching lines")
        linkAdjacentMatchingLines(sourceLines)

        subsequenceLinking(sourceLines, patchLines)

        return Pair(sourceLines, patchLines)
    }

    private fun subsequenceLinking(
        sourceLines: List<LineRecord>,
        patchLines: List<LineRecord>
    ) {
        val sourceBuffer: MutableList<LineRecord> = sourceLines.toMutableList()
        val patchBuffer: MutableList<LineRecord> = patchLines.toMutableList()
        val sourceSegment = mutableListOf<LineRecord>()
        val patchSegment = mutableListOf<LineRecord>()

        while (sourceBuffer.isNotEmpty() && patchBuffer.isNotEmpty()) {
            while (sourceBuffer.isNotEmpty() && sourceBuffer.first().matchingLine == null) {
                sourceSegment.add(sourceBuffer.first())
                sourceBuffer.removeAt(0)
            }
            while (sourceBuffer.isNotEmpty() && sourceBuffer.first().matchingLine != null) {
                sourceBuffer.removeAt(0)
            }
            while (patchBuffer.isNotEmpty() && patchBuffer.first().matchingLine == null) {
                patchSegment.add(patchBuffer.first())
                patchBuffer.removeAt(0)
            }
            while (patchBuffer.isNotEmpty() && patchBuffer.first().matchingLine != null) {
                patchBuffer.removeAt(0)
            }
            if (sourceSegment.isNotEmpty() && patchSegment.isNotEmpty()) {
                if (sourceLines.size == sourceSegment.size) return // No subsequence found
                if (patchLines.size == patchSegment.size) return // No subsequence found
                var matchedLines = linkUniqueMatchingLines(sourceSegment, patchSegment)
                matchedLines += linkAdjacentMatchingLines(sourceSegment)
                if (matchedLines == 0) {
                    matchedLines += matchFirstBrackets(sourceSegment, patchSegment)
                }
                if (matchedLines > 0) subsequenceLinking(sourceSegment, patchSegment)
            }
        }
    }

    /**
     * Generates the final patched text using the links established between the source and patch lines.
     * @param sourceLines The source lines with established links.
     * @param patchLines The patch lines with established links.
     * @return The final patched text.
     */
    private fun generatePatchedTextUsingLinks(sourceLines: List<LineRecord>, patchLines: List<LineRecord>): String {
        log.debug("Starting to generate patched text")

        // Recursive function to generate patched text
        fun generatePatchedText(
            sourceLines: List<LineRecord>,
            patchLines: List<LineRecord>,
            patchedText: List<String>,
            currentMetrics: LineMetrics = LineMetrics()
        ): List<String> {
            // Base cases for recursion
            if (sourceLines.isEmpty() && patchLines.isEmpty()) {
                return patchedText
            }
            if (patchLines.isEmpty()) {
                // Add remaining source lines, checking for bracket mismatches
                return patchedText + sourceLines.mapNotNull { line ->
                    updateMetrics(currentMetrics, line.line ?: "")
                    line.line
                }
            }
            if (sourceLines.isEmpty()) {
                // Add remaining patch lines that are additions
                return patchedText + patchLines.filter { it.type == LineType.ADD }.mapNotNull { line ->
                    updateMetrics(currentMetrics, line.line ?: "")
                    line.line
                }
            }

            val codeLine = sourceLines.first()
            val patchLine = patchLines.first()

            // Helper function to add a line and update metrics
            fun addLine(line: String?): List<String> {
                updateMetrics(currentMetrics, line ?: "")
                return patchedText + (line ?: "")
            }

            // Main logic for generating patched text
            return when {
                patchLine.type == LineType.ADD -> {
                    log.debug("Added new line from patch: $patchLine")
                    generatePatchedText(sourceLines, patchLines.drop(1), addLine(patchLine.line), currentMetrics)
                }

                codeLine.matchingLine == patchLine && patchLine.type == LineType.DELETE -> {
                    log.debug("Skipped deleted line: $codeLine")
                    generatePatchedText(sourceLines.drop(1), patchLines.drop(1), patchedText, currentMetrics)
                }

                codeLine.matchingLine == patchLine -> {
                    if (codeLine.metrics != currentMetrics) {
                        log.warn("Bracket mismatch detected in matched line: ${codeLine.index}")
                    }
                    generatePatchedText(sourceLines.drop(1), patchLines.drop(1), addLine(codeLine.line), currentMetrics)
                }

                codeLine.matchingLine != null -> when (patchLine.type) {
                    LineType.CONTEXT -> generatePatchedText(
                        sourceLines,
                        patchLines.drop(1),
                        addLine(patchLine.line),
                        currentMetrics
                    )

                    else -> generatePatchedText(sourceLines, patchLines.drop(1), patchedText, currentMetrics)
                }

                else -> {
                    log.debug("Added unmatched source line: $codeLine")
                    if (codeLine.metrics != currentMetrics) {
                        log.warn("Bracket mismatch detected in unmatched source line: ${codeLine.index}")
                    }
                    generatePatchedText(sourceLines.drop(1), patchLines, addLine(codeLine.line), currentMetrics)
                }
            }
        }

        val result = generatePatchedText(sourceLines, patchLines, emptyList())
        log.debug("Finished generating patched text")
        return result.joinToString("\n")
    }

    // Updates the bracket metrics for a given line
    private fun updateMetrics(metrics: LineMetrics, line: String) {
        line.forEach { char ->
            when (char) {
                '(' -> metrics.parenthesesDepth++
                ')' -> metrics.parenthesesDepth = maxOf(0, metrics.parenthesesDepth - 1)
                '[' -> metrics.squareBracketsDepth++
                ']' -> metrics.squareBracketsDepth = maxOf(0, metrics.squareBracketsDepth - 1)
                '{' -> metrics.curlyBracesDepth++
                '}' -> metrics.curlyBracesDepth = maxOf(0, metrics.curlyBracesDepth - 1)
            }
        }
    }

    private fun matchFirstBrackets(sourceLines: List<LineRecord>, patchLines: List<LineRecord>): Int {
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
        log.debug("Finished linking matching lines")
        return matched.sumOf { sourceLineMap[it]!!.size }
    }

    /**
     * Links lines between the source and the patch that are unique and match exactly.
     * @param sourceLines The source lines.
     * @param patchLines The patch lines.
     */
    private fun linkUniqueMatchingLines(sourceLines: List<LineRecord>, patchLines: List<LineRecord>): Int {
        log.debug("Starting to link unique matching lines")
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
        log.debug("Finished linking unique matching lines")
        return matched.sumOf { sourceLineMap[it]!!.size }
    }

    /**
     * Links lines that are adjacent to already linked lines and match exactly.
     * @param sourceLines The source lines with some established links.
     */
    private fun linkAdjacentMatchingLines(sourceLines: List<LineRecord>): Int {
        log.debug("Starting to link adjacent matching lines")
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
        log.debug("Finished linking adjacent matching lines")
        return matchedLines
    }

    /**
     * @param text The text to parse.
     * @return The list of line records.
     */
    private fun parseLines(text: String): List<LineRecord> {
        log.debug("Parsing source lines")
        // Create LineRecords for each line and set links between them
        val lines = setLinks(text.lines().mapIndexed { index, line -> LineRecord(index, line) })
        // Calculate bracket metrics for each line
        calculateLineMetrics(lines)
        log.debug("Parsed ${lines.size} source lines")
        return lines
    }

    /**
     * Sets the previous and next line links for a list of line records.
     * @return The list with links set.
     */
    private fun setLinks(list: List<LineRecord>): List<LineRecord> {
        log.debug("Setting links for ${list.size} lines")
        for (i in 0 until list.size) {
            list[i].previousLine = if (i > 0) list[i - 1] else null
            list[i].nextLine = if (i < list.size - 1) list[i + 1] else null
        }
        log.debug("Finished setting links")
        return list
    }

    /**
     * Parses the patch text into a list of line records, identifying the type of each line (ADD, DELETE, CONTEXT).
     * @param text The patch text to parse.
     * @return The list of line records with types set.
     */
    private fun parsePatchLines(text: String): List<LineRecord> {
        log.debug("Parsing patch lines")
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
                }, type = when {
                    line.startsWith("+") -> LineType.ADD
                    line.startsWith("-") -> LineType.DELETE
                    else -> LineType.CONTEXT
                }
            )
        }.filter { it.line != null })
        calculateLineMetrics(patchLines)
        log.debug("Parsed ${patchLines.size} patch lines")
        return patchLines
    }

    /**
     * Calculates the metrics for each line, including bracket nesting depth.
     * @param lines The list of line records to process.
     */
    private fun calculateLineMetrics(lines: List<LineRecord>) {
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

}