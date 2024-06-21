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
        if (original == modified) return modified.withIndex().map { (i, v) -> PatchLine(Unchanged, i, v) }
        val original = original.withIndex().map { (i, v) -> PatchLine(Unchanged, i, v) }.toMutableList()
        val modified = modified.withIndex().map { (i, v) -> PatchLine(Unchanged, i, v) }.toMutableList()
        val patchLines = mutableListOf<PatchLine>()

        while (original.isNotEmpty() && modified.isNotEmpty()) {
            val originalLine = original.first()
            val modifiedLine = modified.first()

            if (originalLine == modifiedLine) {
                patchLines.add(PatchLine(Unchanged, originalLine.lineNumber, originalLine.line))
                original.removeAt(0)
                modified.removeAt(0)
            } else {
                val originalIndex = original.indexOf(modifiedLine).let { if (it == -1) null else it }
                val modifiedIndex = modified.indexOf(originalLine).let { if (it == -1) null else it }

                if (originalIndex != null && modifiedIndex != null) {
                    if (originalIndex < modifiedIndex) {
                        while (original.first() != modifiedLine) {
                            patchLines.add(PatchLine(Deleted, original.first().lineNumber, original.first().line))
                            original.removeAt(0)
                        }
                    } else {
                        while (modified.first() != originalLine) {
                            patchLines.add(PatchLine(Added, modified.first().lineNumber, modified.first().line))
                            modified.removeAt(0)
                        }
                    }
                } else if (originalIndex != null) {
                    while (original.first() != modifiedLine) {
                        patchLines.add(PatchLine(Deleted, original.first().lineNumber, original.first().line))
                        original.removeAt(0)
                    }
                } else if (modifiedIndex != null) {
                    while (modified.first() != originalLine) {
                        patchLines.add(PatchLine(Added, modified.first().lineNumber, modified.first().line))
                        modified.removeAt(0)
                    }
                } else {
                    patchLines.add(PatchLine(Deleted, originalLine.lineNumber, originalLine.line))
                    original.removeAt(0)
                    patchLines.add(PatchLine(Added, modifiedLine.lineNumber, modifiedLine.line))
                    modified.removeAt(0)
                }
            }
        }
        patchLines.addAll(original.map { PatchLine(Deleted, it.lineNumber, it.line) })
        patchLines.addAll(modified.map { PatchLine(Added, it.lineNumber, it.line) })
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
                        patchList.addAll(patchLines.subList(changeStart, idx))
                    }
                    changeEnd = minOf(patchLines.size, idx + contextLines + 1)
                    patchList.add(lineDiff)
                }
                Unchanged -> {
                    if (inChange) {
                        if (idx >= changeEnd) {
                            inChange = false
                            patchList.addAll(patchLines.subList(idx - contextLines, idx))
                        }
                    }
                }
            }
        }

        if (inChange) {
            patchList.addAll(patchLines.subList(changeEnd - contextLines, minOf(changeEnd, patchLines.size)))
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