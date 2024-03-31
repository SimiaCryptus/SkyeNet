package com.github.simiacryptus.aicoder.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DiffUtilTest {

/*
  @Test
  fun testNoChanges() {
    val original = listOf("line1", "line2", "line3")
    val modified = listOf("line1", "line2", "line3")
    val diffResults = DiffUtil.generateDiff(original, modified)
    val formattedDiff = DiffUtil.formatDiff(diffResults)
    assertEquals("", formattedDiff, "There should be no diff for identical inputs.")
  }
*/

//  @Test
  fun testAdditions() {
    val original = listOf("line1", "line2")
    val modified = listOf("line1", "line2", "line3")
    val diffResults = DiffUtil.generateDiff(original, modified)
    val formattedDiff = DiffUtil.formatDiff(diffResults)
    val expectedDiff = """
              line1
              line2
            + line3
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
            - line2
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
            - line2
            + line3
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
            - line2
            + changed_line2
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
             - line1
             + changed_line1
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
             - line2
             + changed_line2
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
             - line2
             + changed_line2
               line3
         """.trimIndent()
    assertEquals(expectedDiff, formattedDiff, "The diff should correctly handle cases with no context lines.")
  }

  @Test
  fun testVerifyLLMPatch() {
    val originalCode = """
      /* Basic reset for styling */
      * {
        margin: 0;
        padding: 0;
        box-sizing: border-box;
      }

      body {
        font-family: 'Arial', sans-serif;
        background-color: #f0f0f0;
        display: flex;
        justify-content: center;
        align-items: center;
        height: 100vh;
      }

      .game-container {
        background-color: #ffffff;
        padding: 20px;
        border-radius: 8px;
        box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
      }

      .grid {
        display: grid;
        grid-template-columns: repeat(8, 50px);
        grid-gap: 10px;
        justify-content: center;
        margin-bottom: 20px;
      }

      .grid div {
        width: 50px;
        height: 50px;
        background-color: #e0e0e0;
        display: flex;
        justify-content: center;
        align-items: center;
        font-size: 24px;
        font-weight: bold;
        cursor: pointer;
        user-select: none;
      }

      .scoreboard {
        margin-bottom: 20px;
      }

      .timer, .score {
        font-size: 20px;
        margin-bottom: 10px;
      }

      button {
        padding: 10px 20px;
        font-size: 16px;
        cursor: pointer;
        border: none;
        border-radius: 5px;
        background-color: #007bff;
        color: white;
      }

      button:hover {
        background-color: #0056b3;
      }

      .instructions {
        font-size: 14px;
        color: #666;
        text-align: center;
        margin-top: 20px;
      }
    """.trimIndent()
    val llmPatch = """
        body {
          font-family: 'Arial', sans-serif;
      -   background-color: #f0f0f0;
      +   background-color: #f7f7f7;
      +   background-image: linear-gradient(315deg, #f7f7f7 0%, #c2e9fb 74%);
          display: flex;
          justify-content: center;
          align-items: center;
          height: 100vh;
        }
      
        .game-container {
          background-color: #ffffff;
          padding: 20px;
          border-radius: 8px;
          box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
      +   transition: transform 0.3s ease;
        }
      
      + .game-container:hover {
      +   transform: scale(1.02);
      + }
      
        .grid div {
          width: 50px;
          height: 50px;
      -   background-color: #e0e0e0;
      +   background-color: #d6e4ff;
          display: flex;
          justify-content: center;
          align-items: center;
          font-size: 24px;
          font-weight: bold;
          cursor: pointer;
          user-select: none;
      +   border-radius: 5px;
      +   transition: background-color 0.3s ease;
        }
      
      + .grid div:hover {
      +   background-color: #adc8ff;
      + }
      
        button {
          padding: 10px 20px;
          font-size: 16px;
          cursor: pointer;
          border: none;
          border-radius: 5px;
      -   background-color: #007bff;
      +   background-color: #4CAF50;
          color: white;
      +   box-shadow: 0 4px #259227;
        }
      
        button:hover {
      -   background-color: #0056b3;
      +   background-color: #45a049;
        }
      
      + button:active {
      +   background-color: #3e8e41;
      +   box-shadow: 0 2px #666;
      +   transform: translateY(2px);
      + }
    """.trimIndent()
    val reconstructed = ApxPatchUtil.patch(originalCode, llmPatch)

    val patchLines = DiffUtil.generateDiff(originalCode.lines(), reconstructed.lines())
//    println("\n\nPatched:\n\n")
//    patchLines.forEach { println(it) }

    println("\n\nEcho Patch:\n\n")
    DiffUtil.formatDiff(patchLines).lines().forEach { println(it) }
  }
}
