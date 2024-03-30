package com.github.simiacryptus.aicoder.util

import com.github.simiacryptus.aicoder.util.PatchLineType.*

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
    if (original == modified) return modified.withIndex().map { (i,v) -> PatchLine(Unchanged, i, v) }
    val original = original.withIndex().map { (i,v) -> PatchLine(Unchanged, i, v) }.toMutableList()
    val modified = modified.withIndex().map { (i,v) -> PatchLine(Unchanged, i, v) }.toMutableList()
    val patchLines = mutableListOf<PatchLine>()

    while (original.isNotEmpty() && modified.isNotEmpty()) {
      val originalLine = original.first()
      val modifiedLine = modified.first()

      if (originalLine == modifiedLine) {
        patchLines.add(PatchLine(Unchanged, originalLine.lineNumber, originalLine.line))
        original.removeAt(0)
        modified.removeAt(0)
      } else {
        val originalIndex = original.indexOf(modifiedLine)
        val modifiedIndex = modified.indexOf(originalLine)

        if (originalIndex != -1) {
          for (i in 0 until originalIndex) {
            patchLines.add(PatchLine(Deleted, original[i].lineNumber, original[i].line))
          }
          patchLines.add(PatchLine(Added, modifiedLine.lineNumber, modifiedLine.line))
          original.removeAll { it == modifiedLine }
          modified.removeAt(0)
        } else if (modifiedIndex != -1) {
          for (i in 0 until modifiedIndex) {
            patchLines.add(PatchLine(Added, modified[i].lineNumber, modified[i].line))
          }
          patchLines.add(PatchLine(Deleted, originalLine.lineNumber, originalLine.line))
          modified.removeAll { it == originalLine }
          original.removeAt(0)
        } else {
          patchLines.add(PatchLine(Deleted, originalLine.lineNumber, originalLine.line))
          patchLines.add(PatchLine(Added, modifiedLine.lineNumber, modifiedLine.line))
          original.removeAt(0)
          modified.removeAt(0)
        }
      }
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
  fun formatDiff(patchLines: List<PatchLine>, contextLines: Int = 3) =
    patchLines.withIndex().filter { (idx, lineDiff) ->
      when(lineDiff.type) {
        Added -> true
        Deleted -> true
        Unchanged -> {
          val distBackwards = idx - patchLines.subList(0, idx).indexOfLast { it.type != Unchanged }
          val distForwards = patchLines.subList(idx, patchLines.size).indexOfFirst { it.type != Unchanged }
          distBackwards <= contextLines || distForwards <= contextLines
        }
      }
    }.joinToString("\n") { (idx, lineDiff) ->
      when(lineDiff.type) {
        Added -> "+ ${lineDiff.line}"
        Deleted -> "- ${lineDiff.line}"
        Unchanged -> "  ${lineDiff.line}"
      }
    }
}
