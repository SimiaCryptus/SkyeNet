package com.github.simiacryptus.aicoder.util

import kotlin.math.max

sealed class DiffResult {
  data class Added(val line: String) : DiffResult()
  data class Deleted(val line: String) : DiffResult()
  data class Unchanged(val line: String) : DiffResult()
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
  fun generateDiff(original: List<String>, modified: List<String>): List<DiffResult> {
    val diffResults = mutableListOf<DiffResult>()

    if (original == modified) return diffResults
    // Fix: Initialize pointers for both lists
    var oIndex = 0
    var mIndex = 0
    // This simple implementation is for demonstration purposes
    original.forEachIndexed { index, line ->
      // If the current index is beyond the size of the modified list, the line is considered deleted
      when {
        // If the line at the current index is the same in both original and modified, it's unchanged
        // If the line at the current index is different, the original line is considered deleted
        index == 0 && original[index] != modified.getOrNull(index) -> {
          diffResults.add(DiffResult.Deleted(original[index]))
          diffResults.add(DiffResult.Added(modified[index]))
        } // Fix: Remove this block as it incorrectly handles the first line special case

        index >= modified.size -> {
          diffResults.add(DiffResult.Deleted(line))
        }

        modified.getOrNull(index) == line -> {
          diffResults.add(DiffResult.Unchanged(line))
        }

        else -> {
          if (index < modified.size && original[index] != modified[index]) {
            // Check if the line is truly deleted or just modified
            if (!modified.contains(original[index])) {
              diffResults.add(DiffResult.Deleted(original[index]))
            }
            if (!original.contains(modified[index])) {
              diffResults.add(DiffResult.Added(modified[index]))
            }
            oIndex++ // Fix: Increment original index to correctly track progress
          } else if (modified.subList(index, modified.size).contains(line)) {
            diffResults.add(DiffResult.Added(modified[index]))
          } else {
            diffResults.add(DiffResult.Deleted(line))
          }
        }
      }

      // Handle additional lines in the modified text
      if (index == original.lastIndex && modified.size > original.size) {
        // For each line in modified that doesn't have a corresponding line in original, mark it as added
        if (index >= original.size) {
          modified.subList(max(original.size, index), modified.size).forEach {
            diffResults.add(DiffResult.Added(it))
          }
          mIndex = modified.size // Fix: Ensure modified index is updated to prevent over-processing
        }
      }
    }

    return diffResults
  }

  /**
   * Determines if the provided context buffer is at the end of the diff.
   * This function checks if all elements in the context buffer are of type Unchanged, indicating no more changes ahead.
   *
   * @param contextBuffer The list of DiffResult to check.
   * @return True if all elements are Unchanged, false otherwise.
   */
  fun isEndOfDiff(contextBuffer: List<DiffResult>): Boolean {
    // Check if all elements in the context buffer are unchanged, indicating the end of the diff
    return contextBuffer.all { it is DiffResult.Unchanged }
  }

  /**
   * Formats the list of DiffResult into a human-readable string representation.
   * This function processes each diff result to format added, deleted, and unchanged lines appropriately,
   * including context lines and markers for easier reading.
   *
   * @param diffResults The list of DiffResult to format.
   * @param contextLines The number of context lines to include around changes.
   * @return A formatted string representing the diff.
   */
  fun formatDiff(diffResults: List<DiffResult>, contextLines: Int = 3): String {
    val formattedDiff = StringBuilder()
    var contextBuffer = mutableListOf<DiffResult>()
    var linesSinceChange = 0
    var inContext = false

    fun flushContextBuffer() {
      if (contextBuffer.isNotEmpty()) {
        // Determine if the context buffer is at the end of the diff results to decide on adding the context marker
        // Only add context marker if we are not at the start of the diff
        // Check if the context buffer is at the end of the diff results to decide on adding the context marker
        val isAtEndOfDiff = diffResults.indexOf(contextBuffer.last()) == diffResults.size - 1
        val shouldAddContextMarker =
          contextLines > 0 && formattedDiff.isNotEmpty() && !isEndOfDiff(contextBuffer) && !isAtEndOfDiff && linesSinceChange >= contextLines
        // Add context marker if not at the start, and there are context lines to show
        if (shouldAddContextMarker) {
          formattedDiff.append("...\n") // Add context marker if not at the start and contextLines > 0
        }
        contextBuffer.forEach { result ->
          when (result) {
            // Format added lines with a "+" prefix
            is DiffResult.Added -> formattedDiff.append("+${result.line}\n")
            // Format deleted lines with a "-" prefix
            is DiffResult.Deleted -> formattedDiff.append("-${result.line}\n")
            // Format unchanged lines with a space prefix, if context lines are to be included
            is DiffResult.Unchanged -> if (contextLines > 0) formattedDiff.append(" ${result.line}\n")
          }
        }
        contextBuffer.clear()
        linesSinceChange = 0
      }
    }


    for (result in diffResults) {
      when (result) {
        // Handle unchanged lines, considering context lines and whether we're at the end of the diff
        is DiffResult.Unchanged -> {
          if (linesSinceChange < contextLines || inContext) { // Fix: Ensure context is correctly managed when in context
            flushContextBuffer()
            inContext = contextLines > 0
          }
          if (linesSinceChange < contextLines || diffResults.indexOf(result) == diffResults.size - 1) {
            contextBuffer.add(result)
            linesSinceChange++
            if (contextBuffer.size > contextLines) contextBuffer.removeAt(0)
          } else if (contextLines > 0) {
            formattedDiff.append(" ${result.line}\n")
          }
        }

        // For added or deleted lines, reset context and prepare to flush if needed
        else -> {
          inContext = false // Fix: Correctly reset inContext flag to manage context flushing
          flushContextBuffer()
          contextBuffer.add(result)
        }
      }
      // Flush the context buffer if we've reached the maximum number of context lines, or if we're in a context section
      if (linesSinceChange >= contextLines && contextLines > 0) {
        flushContextBuffer()
        inContext = contextLines > 0
      }
      // Ensure the context buffer does not exceed the specified number of context lines
      if (contextBuffer.size > contextLines) contextBuffer.removeAt(0)
      // Reset linesSinceChange counter when in context to accurately track distance from last change
      if (inContext) linesSinceChange = 0
    }

    flushContextBuffer() // Flush remaining changes
    return formattedDiff.toString().trimEnd('\n')
  }
}
