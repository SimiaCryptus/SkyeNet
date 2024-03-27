package com.github.simiacryptus.aicoder.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DiffUtilTest {

  @Test
  fun testNoChanges() {
    val original = listOf("line1", "line2", "line3")
    val modified = listOf("line1", "line2", "line3")
    val diffResults = DiffUtil.generateDiff(original, modified)
    val formattedDiff = DiffUtil.formatDiff(diffResults)
    assertEquals("", formattedDiff, "There should be no diff for identical inputs.")
  }

//  @Test
  fun testAdditions() {
    val original = listOf("line1", "line2")
    val modified = listOf("line1", "line2", "line3")
    val diffResults = DiffUtil.generateDiff(original, modified)
    val formattedDiff = DiffUtil.formatDiff(diffResults)
    val expectedDiff = """
             line1
             line2
            +line3
        """.trimIndent()
    assertEquals(expectedDiff, formattedDiff, "The diff should correctly represent an addition.")
  }

//  @Test
  fun testDeletions() {
    val original = listOf("line1", "line2", "line3")
    val modified = listOf("line1", "line3")
    val diffResults = DiffUtil.generateDiff(original, modified)
    val formattedDiff = DiffUtil.formatDiff(diffResults)
    val expectedDiff = """
             line1
            -line2
             line3
        """.trimIndent()
    assertEquals(expectedDiff, formattedDiff, "The diff should correctly represent a deletion.")
  }

  @Test
  fun testMixedChanges() {
    val original = listOf("line1", "line2", "line4")
    val modified = listOf("line1", "line3", "line4")
    val diffResults = DiffUtil.generateDiff(original, modified)
    val formattedDiff = DiffUtil.formatDiff(diffResults)
    val expectedDiff = """
             line1
            -line2
            +line3
             line4
        """.trimIndent()
    assertEquals(expectedDiff, formattedDiff, "The diff should correctly represent mixed changes.")
  }

//  @Test
  fun testContextLines() {
    val original = listOf("line0", "line1", "line2", "line3", "line4")
    val modified = listOf("line0", "line1", "changed_line2", "line3", "line4")
    val diffResults = DiffUtil.generateDiff(original, modified)
    val formattedDiff = DiffUtil.formatDiff(diffResults, 1)
    val expectedDiff = """
             line1
            -line2
            +changed_line2
             line3
        """.trimIndent()
    assertEquals(expectedDiff, formattedDiff, "The diff should correctly include context lines.")
  }

  @Test
  fun testStartWithChange() {
    val original = listOf("line1", "line2")
    val modified = listOf("changed_line1", "line2")
    val diffResults = DiffUtil.generateDiff(original, modified)
    val formattedDiff = DiffUtil.formatDiff(diffResults)
    val expectedDiff = """
             -line1
             +changed_line1
              line2
         """.trimIndent()
    assertEquals(expectedDiff, formattedDiff, "The diff should correctly represent changes at the start.")
  }

  @Test
  fun testEndWithChange() {
    val original = listOf("line1", "line2")
    val modified = listOf("line1", "changed_line2")
    val diffResults = DiffUtil.generateDiff(original, modified)
    val formattedDiff = DiffUtil.formatDiff(diffResults)
    val expectedDiff = """
              line1
             -line2
             +changed_line2
         """.trimIndent()
    assertEquals(expectedDiff, formattedDiff, "The diff should correctly represent changes at the end.")
  }

//  @Test
  fun testNoContextNeeded() {
    val original = listOf("line1", "line2", "line3")
    val modified = listOf("line1", "changed_line2", "line3")
    val diffResults = DiffUtil.generateDiff(original, modified)
    val formattedDiff = DiffUtil.formatDiff(diffResults, 0)
    val expectedDiff = """
              line1
             -line2
             +changed_line2
              line3
         """.trimIndent()
    assertEquals(expectedDiff, formattedDiff, "The diff should correctly handle cases with no context lines.")
  }
}
