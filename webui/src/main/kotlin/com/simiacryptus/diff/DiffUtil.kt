package com.simiacryptus.diff

import com.simiacryptus.diff.PatchLineType.*

enum class PatchLineType {
    Added, Deleted, Unchanged
}

data class PatchLine(
    val type: PatchLineType,
    val lineNumber: Int,
    val line: String,
    val compareText: String = line.trim(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PatchLine

        return compareText == other.compareText
    }

    override fun hashCode(): Int {
        return compareText.hashCode()
    }
}

object DiffUtil {

    /**
     * Generates a list of DiffResult representing the differences between two lists of strings.
     * This function compares the original and modified texts line by line and categorizes each line as added, deleted, or unchanged.
     *
     * @param original The original list of strings.
     * @param modified The modified list of strings.
     * @return A list of DiffResult indicating the differences.
     */
    fun generateDiff(original: List<String>, modified: List<String>): List<PatchLine> {
        val originalLines = original.withIndex().map { (i, v) -> PatchLine(Unchanged, i, v) }
        val modifiedLines = modified.withIndex().map { (i, v) -> PatchLine(Unchanged, i, v) }
        val patchLines = mutableListOf<PatchLine>()
        var i = 0
        var j = 0

        while (i < originalLines.size && j < modifiedLines.size) {
            val originalLine = originalLines[i]
            val modifiedLine = modifiedLines[j]

            if (originalLine == modifiedLine) {
                patchLines.add(PatchLine(Unchanged, originalLine.lineNumber, originalLine.line))
                i++
                j++
            } else {
                val originalIndex = originalLines.subList(i, originalLines.size).indexOf(modifiedLine).let { if (it == -1) null else it + i }
                val modifiedIndex = modifiedLines.subList(j, modifiedLines.size).indexOf(originalLine).let { if (it == -1) null else it + j }

                if (originalIndex != null && modifiedIndex != null) {
                    if (originalIndex < modifiedIndex) {
                        while (i < originalIndex) {
                            patchLines.add(PatchLine(Deleted, originalLines[i].lineNumber, originalLines[i].line))
                            i++
                        }
                    } else {
                        while (j < modifiedIndex) {
                            patchLines.add(PatchLine(Added, modifiedLines[j].lineNumber, modifiedLines[j].line))
                            j++
                        }
                    }
                } else if (originalIndex != null) {
                    while (i < originalIndex) {
                        patchLines.add(PatchLine(Deleted, originalLines[i].lineNumber, originalLines[i].line))
                        i++
                    }
                } else if (modifiedIndex != null) {
                    while (j < modifiedIndex) {
                        patchLines.add(PatchLine(Added, modifiedLines[j].lineNumber, modifiedLines[j].line))
                        j++
                    }
                } else {
                    patchLines.add(PatchLine(Deleted, originalLines[i].lineNumber, originalLines[i].line))
                    patchLines.add(PatchLine(Added, modifiedLines[j].lineNumber, modifiedLines[j].line))
                    i++
                    j++
                }
            }
        }
        
        while (i < originalLines.size) {
            patchLines.add(PatchLine(Deleted, originalLines[i].lineNumber, originalLines[i].line))
            i++
        }
        
        while (j < modifiedLines.size) {
            patchLines.add(PatchLine(Added, modifiedLines[j].lineNumber, modifiedLines[j].line))
            j++
        }
        
        return patchLines
    }

    /**
     * Formats the list of DiffResult into a human-readable string representation.
     * This function processes each diff result to format added, deleted, and unchanged lines appropriately,
     * including context lines and markers for easier reading.
     *
     * @param patchLines The list of DiffResult to format.
     * @param contextLines The number of context lines to include around changes.
     * @return A formatted string representing the diff.
     */
    fun formatDiff(patchLines: List<PatchLine>, contextLines: Int = 3): String {
        val patchList = mutableListOf<PatchLine>()
        var inChange = false
        var changeStart = 0
        var changeEnd = 0

        patchLines.forEachIndexed { idx, lineDiff ->
            when (lineDiff.type) {
                Added, Deleted -> {
                    if (!inChange) {
                        inChange = true
                        changeStart = maxOf(0, idx - contextLines)
                        patchList.addAll(patchLines.subList(changeStart, idx).filter { it.type == Unchanged })
                    }
                    changeEnd = minOf(patchLines.size, idx + contextLines + 1)
                    patchList.add(lineDiff)
                }
                Unchanged -> {
                    if (inChange) {
                        if (idx >= changeEnd) {
                            inChange = false
                            patchList.addAll(patchLines.subList(maxOf(changeEnd, idx - contextLines), idx))
                        }
                    }
                }
            }
        }

        if (inChange) {
            patchList.addAll(patchLines.subList(maxOf(changeEnd - contextLines, patchList.size), minOf(changeEnd, patchLines.size)))
        }

        return patchList.joinToString("\n") { lineDiff ->
            when (lineDiff.type) {
                Added -> "+ ${lineDiff.line}"
                Deleted -> "- ${lineDiff.line}"
                Unchanged -> "  ${lineDiff.line}"
            }
        }
    }
}