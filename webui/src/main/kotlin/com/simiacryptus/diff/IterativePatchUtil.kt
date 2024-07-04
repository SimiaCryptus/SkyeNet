package com.simiacryptus.diff

import org.slf4j.LoggerFactory

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
        // Compare old and new code to establish links between matching lines
        val (oldLines, newLines) = compare(oldCode, newCode)
        // Create a patch string based on the comparison results
        return createPatchFromComparison(oldLines, newLines)
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

        // Iterate through both old and new lines to create the patch
        while (oldIndex < oldLines.size || newIndex < newLines.size) {
            when {
                oldIndex >= oldLines.size -> {
                    // Remaining lines in new code are additions
                    patchBuilder.append("+ ${newLines[newIndex].line}\n")
                    newIndex++
                }

                newIndex >= newLines.size -> {
                    // Remaining lines in old code are deletions
                    patchBuilder.append("- ${oldLines[oldIndex].line}\n")
                    oldIndex++
                }

                oldLines[oldIndex].matchingLine == newLines[newIndex] -> {
                    // Matching lines are context
                    patchBuilder.append("  ${oldLines[oldIndex].line}\n")
                    oldIndex++
                    newIndex++
                }

                else -> {
                    // Non-matching lines are either deletions or additions
                    if (oldLines[oldIndex].matchingLine == null) {
                        patchBuilder.append("- ${oldLines[oldIndex].line}\n")
                        oldIndex++
                    } else {
                        patchBuilder.append("+ ${newLines[newIndex].line}\n")
                        newIndex++
                    }
                }
            }
        }

        log.debug("Patch creation completed")
        return patchBuilder.toString()
    }

    /**
     * Applies a patch to the given source text.
     * @param source The original text.
     * @param patch The patch to apply.
     * @return The text after the patch has been applied.
     */
    fun patch(source: String, patch: String): String {
        // Compare source and patch to establish links between matching lines
        val (sourceLines, patchLines) = compare(source, patch)

        // Generate the patched text using the established links
        log.info("Generating patched text using established links")
        val generatePatchedTextUsingLinks = generatePatchedTextUsingLinks(sourceLines, patchLines)

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
        return Pair(sourceLines, patchLines)
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

                codeLine.matchingLine != null -> when(patchLine.type) {
                    LineType.CONTEXT -> generatePatchedText(sourceLines, patchLines.drop(1), addLine(patchLine.line), currentMetrics)
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

    /**
     * Links lines between the source and the patch that are unique and match exactly.
     * @param sourceLines The source lines.
     * @param patchLines The patch lines.
     */
    private fun linkUniqueMatchingLines(sourceLines: List<LineRecord>, patchLines: List<LineRecord>) {
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
        sourceLineMap.keys.intersect(patchLineMap.keys).forEach { key ->
            val sourceLine = sourceLineMap[key]?.singleOrNull()
            val patchLine = patchLineMap[key]?.singleOrNull()
            if (sourceLine != null && patchLine != null) {
                sourceLine.matchingLine = patchLine
                patchLine.matchingLine = sourceLine
                log.debug("Linked unique matching lines: Source[${sourceLine.index}]: ${sourceLine.line} <-> Patch[${patchLine.index}]: ${patchLine.line}")
            }
        }
        log.debug("Finished linking unique matching lines")
    }

    /**
     * Links lines that are adjacent to already linked lines and match exactly.
     * @param sourceLines The source lines with some established links.
     */
    private fun linkAdjacentMatchingLines(sourceLines: List<LineRecord>) {
        log.debug("Starting to link adjacent matching lines")
        var foundMatch = true
        // Continue linking until no more matches are found
        while (foundMatch) {
            log.debug("Starting new iteration to find adjacent matches")
            foundMatch = false
            for (sourceLine in sourceLines) {
                val patchLine = sourceLine.matchingLine ?: continue // Skip if there's no matching line

                // Check the previous line for a potential match
                if (sourceLine.previousLine != null && patchLine.previousLine != null) {
                    val sourcePrev = sourceLine.previousLine!!
                    var patchPrev = patchLine.previousLine!!
                    // Skip ADD lines in the patch when looking for matches
                    while (patchPrev.type == LineType.ADD && patchPrev.previousLine != null) {
                        patchPrev = patchPrev.previousLine!!
                    }
                    if (sourcePrev.matchingLine == null && patchPrev.matchingLine == null) { // Skip if there's already a match
                        if (normalizeLine(sourcePrev.line!!) == normalizeLine(patchPrev.line!!)) { // Check if the lines match exactly
                            sourcePrev.matchingLine = patchPrev
                            patchPrev.matchingLine = sourcePrev
                            foundMatch = true
                            log.debug("Linked adjacent previous lines: Source[${sourcePrev.index}]: ${sourcePrev.line} <-> Patch[${patchPrev.index}]: ${patchPrev.line}")
                        }
                    }
                }

                // Check the next line for a potential match
                if (sourceLine.nextLine != null && patchLine.nextLine != null) {
                    val sourceNext = sourceLine.nextLine!!
                    var patchNext = patchLine.nextLine!!
                    // Skip ADD lines in the patch when looking for matches
                    while (patchNext.type == LineType.ADD && patchNext.nextLine != null) {
                        patchNext = patchNext.nextLine!!
                    }
                    if (sourceNext.matchingLine == null && patchNext.matchingLine == null) {
                        if (normalizeLine(sourceNext.line!!) == normalizeLine(patchNext.line!!)) {
                            sourceNext.matchingLine = patchNext
                            patchNext.matchingLine = sourceNext
                            foundMatch = true
                            log.debug("Linked adjacent next lines: Source[${sourceNext.index}]: ${sourceNext.line} <-> Patch[${patchNext.index}]: ${patchNext.line}")
                        }
                    }
                }
            }
        }
        log.debug("Finished linking adjacent matching lines")
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

}