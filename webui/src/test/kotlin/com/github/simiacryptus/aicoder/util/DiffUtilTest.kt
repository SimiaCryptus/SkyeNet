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
      <!DOCTYPE html>
      <html lang="en">
      <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Parakeet Paradise</title>
          <link rel="stylesheet" href="styles.css">
          <script src="scripts.js" defer></script>
      </head>
      <body>
          <header>
              <nav>
                  <div class="logo">
                      <h1>Parakeet Paradise</h1>
                  </div>
                  <ul class="nav-links">
                      <li><a href="index.html">Home</a></li>
                      <li><a href="about-parakeets.html">About Parakeets</a></li>
                      <li><a href="care-guide.html">Care Guide</a></li>
                      <li><a href="breeds.html">Breeds</a></li>
                      <li><a href="gallery.html">Gallery</a></li>
                      <li><a href="faqs.html">FAQs</a></li>
                      <li><a href="contact-us.html">Contact Us</a></li>
                      <li><a href="blog.html">Blog</a></li>
                  </ul>
              </nav>
          </header>
          <main>
              <section class="hero">
                  <h2>Welcome to Parakeet Paradise!</h2>
                  <p>Discover the colorful world of parakeets and learn everything you need to know about these delightful birds.</p>
              </section>
              <section class="featured-content">
                  <article>
                      <h3>Parakeet of the Month</h3>
                      <p>Meet Charlie, a vibrant and playful Budgerigar who loves to sing and interact with his human family.</p>
                  </article>
                  <article>
                      <h3>Latest Blog Posts</h3>
                      <ul>
                          <li><a href="blog-post-1.html">5 Fun Facts About Parakeets</a></li>
                          <li><a href="blog-post-2.html">How to Train Your Parakeet to Talk</a></li>
                          <li><a href="blog-post-3.html">The Best Diet for Healthy Parakeets</a></li>
                      </ul>
                  </article>
                  <article>
                      <h3>Care Tips</h3>
                      <p>Learn how to provide the best care for your feathered friend, from diet to daily routines.</p>
                  </article>
              </section>
          </main>
          <footer>
              <p>&copy; 2023 Parakeet Paradise. All rights reserved.</p>
              <p>Follow us on <a href="#">Social Media</a></p>
          </footer>
      </body>
      </html>
    """.trimIndent()
    val llmPatch = """
      <!DOCTYPE html>
      <html lang="en">
      <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Parakeet Paradise</title>
      +   <!-- Link to the stylesheet -->
          <link rel="stylesheet" href="styles.css">
      +   <!-- Link to the JavaScript file -->
          <script src="scripts.js" defer></script>
      </head>
      <body>
          <header>
              <nav>
                  <div class="logo">
                      <h1>Parakeet Paradise</h1>
                  </div>
                  <ul class="nav-links">
                      <li><a href="index.html">Home</a></li>
                      <li><a href="about-parakeets.html">About Parakeets</a></li>
                      <li><a href="care-guide.html">Care Guide</a></li>
                      <li><a href="breeds.html">Breeds</a></li>
                      <li><a href="gallery.html">Gallery</a></li>
                      <li><a href="faqs.html">FAQs</a></li>
                      <li><a href="contact-us.html">Contact Us</a></li>
                      <li><a href="blog.html">Blog</a></li>
                  </ul>
              </nav>
          </header>
          <main>
              <section class="hero">
                  <h2>Welcome to Parakeet Paradise!</h2>
                  <p>Discover the colorful world of parakeets and learn everything you need to know about these delightful birds.</p>
              </section>
              <section class="featured-content">
                  <article>
                      <h3>Parakeet of the Month</h3>
                      <p>Meet Charlie, a vibrant and playful Budgerigar who loves to sing and interact with his human family.</p>
                  </article>
                  <article>
                      <h3>Latest Blog Posts</h3>
                      <ul>
                          <li><a href="blog-post-1.html">5 Fun Facts About Parakeets</a></li>
                          <li><a href="blog-post-2.html">How to Train Your Parakeet to Talk</a></li>
                          <li><a href="blog-post-3.html">The Best Diet for Healthy Parakeets</a></li>
                      </ul>
                  </article>
                  <article>
                      <h3>Care Tips</h3>
                      <p>Learn how to provide the best care for your feathered friend, from diet to daily routines.</p>
                  </article>
              </section>
          </main>
          <footer>
              <p>© 2023 Parakeet Paradise. All rights reserved.</p>
              <p>Follow us on <a href="#">Social Media</a></p>
          </footer>
      </body>
      </html>
    """.trimIndent()
    val reconstructed = ApxPatchUtil.patch(originalCode, llmPatch)

    val patchLines = DiffUtil.generateDiff(originalCode.lines(), reconstructed.lines())
    println("\n\nPatched:\n\n")
    patchLines.forEach {
      println(it)
    }

    println("\n\nEcho Patch:\n\n")
    DiffUtil.formatDiff(patchLines).lines().forEach {
      println(it)
    }
  }
}
