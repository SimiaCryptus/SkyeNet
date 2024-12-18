package com.simiacryptus.diff

import com.simiacryptus.diff.FileValidationUtils.Companion.isGitignore
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.models.ChatModel
import com.simiacryptus.jopenai.models.OpenAIModels
import com.simiacryptus.skyenet.AgentPatterns.displayMapInTabs
import com.simiacryptus.skyenet.core.actors.ParsedActor
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.set
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SocketManagerBase
import com.simiacryptus.util.JsonUtil
import java.io.File
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.readText


// Function to reverse the order of lines in a string
private fun String.reverseLines(): String = lines().reversed().joinToString("\n")

// Main function to add apply file diff links to the response
fun SocketManagerBase.addApplyFileDiffLinks(
  root: Path,
  response: String,
  handle: (Map<Path, String>) -> Unit = {},
  ui: ApplicationInterface,
  api: API,
  shouldAutoApply: (Path) -> Boolean = { false },
  model: ChatModel? = null,
  defaultFile: String? = null,
): String {
  // Check if there's an unclosed code block and close it if necessary
  val initiator = "(?s)```\\w*\n".toRegex()
  if (response.contains(initiator) && !response.split(initiator, 2)[1].contains("\n```(?![^\n])".toRegex())) {
    // Single diff block without the closing ``` due to LLM limitations... add it back and recurse
    return addApplyFileDiffLinks(
      root,
      response + "\n```\n",
      handle,
      ui,
      api,
      model = model,
      defaultFile = defaultFile,
    )
  }
  val headerPattern = """(?<![^\n])#+\s*([^\n]+)""".toRegex() // capture filename
  val codeblockPattern = """(?s)(?<![^\n])```([^\n]*)(\n.*?\n)```""".toRegex() // capture filename
  val headers = headerPattern.findAll(response).map { it.range to it.groupValues[1] }.toList()
  val findAll = codeblockPattern.findAll(response).toList()
  val codeblocks = findAll.filter { block ->
    try {
      val header = headers.lastOrNull { it.first.last <= block.range.first }?.let { it.second } ?: defaultFile
      if (header == null) {
        return@filter false
      }
      val filename = resolve(root, header)
      !root.resolve(filename).toFile().exists()
    } catch (e: Throwable) {
      log.info("Error processing code block", e)
      false
    }
  }.map { it.range to it }.toList()
  val patchBlocks = findAll.filter { block ->
    try {
      val header = headers.lastOrNull { it.first.last <= block.range.first }?.let { it.second } ?: defaultFile
      if (header == null) {
        return@filter false
      }
      val filename = resolve(root, header)
      root.resolve(filename).toFile().exists()
    } catch (e: Throwable) {
      log.info("Error processing code block", e)
      false
    }
  }.map { it.range to it }.toList()

  // Get
  val changes = patchBlocks.mapIndexed { index, it ->
    PatchOrCode(
      id = "patch_" + index.toString(),
      type = "patch",
      data = it.second.groupValues[2]
    )
  } + codeblocks.mapIndexed { index, it ->
    PatchOrCode(
      id = "code_" + index.toString(),
      type = "code",
      data = it.second.groupValues[2]
    )
  }
  val corrections = if (model == null) null else try {
    ParsedActor<CorrectedPatchAndCodeList>(
      resultClass = CorrectedPatchAndCodeList::class.java,
      exampleInstance = CorrectedPatchAndCodeList(
        listOf(
          CorrectedPatchOrCode("patch_0", "src/utils/exampleUtils.js"),
          CorrectedPatchOrCode("code_0", "src/utils/exampleUtils.js"),
          CorrectedPatchOrCode("patch_1", "tests/exampleUtils.test.js"),
          CorrectedPatchOrCode("code_1", "tests/exampleUtils.test.js"),
        )
      ),
      prompt = """
                Review and correct the file path assignments for the following patches and code blocks.
            """.trimIndent(),
      model = model,
      temperature = 0.0,
      parsingModel = model,
    ).getParser(api).apply(
      listOf(
        response,
        JsonUtil.toJson(
          PatchAndCodeList(
            changes = changes
          )
        )
      ).joinToString("\n\n")
    ).changes?.associateBy { it.id }?.mapValues { it.value.filename } ?: emptyMap()
  } catch (e: Throwable) {
    log.error("Error consulting AI for corrections", e)
    null
  }

  // Process diff blocks and add patch links
  val withPatchLinks: String = patchBlocks.foldIndexed(response) { index, markdown, diffBlock ->
    val value = diffBlock.second.groupValues[2].trim()
    var header = headers.lastOrNull { it.first.last < diffBlock.first.first }?.second ?: defaultFile ?: "Unknown"
    header = corrections?.get("patch_$index") ?: header
    val filename = resolve(root, header)
    val newValue = renderDiffBlock(root, filename, value, handle, ui, api, shouldAutoApply)
    markdown.replace(diffBlock.second.value, newValue)
  }
  // Process code blocks and add save links
  val withSaveLinks = codeblocks.foldIndexed(withPatchLinks) { index, markdown, codeBlock ->
    val lang = codeBlock.second.groupValues[1]
    val value = codeBlock.second.groupValues[2].trim()
    var header = headers.lastOrNull { it.first.last < codeBlock.first.first }?.second ?: defaultFile ?: "Unknown"
    header = corrections?.get("code_$index") ?: header
    val newMarkdown = renderNewFile(header, root, ui, shouldAutoApply, value, handle, lang)
    markdown.replace(codeBlock.second.value, newMarkdown)
  }
  return withSaveLinks
}

data class PatchAndCodeList(
  val changes: List<PatchOrCode>,
)

data class PatchOrCode(
  val id: String? = null,
  val type: String? = null,
  val filename: String? = null,
  val data: String? = null,
)

data class CorrectedPatchAndCodeList(
  val changes: List<CorrectedPatchOrCode>? = null,
)

data class CorrectedPatchOrCode(
  val id: String? = null,
  val filename: String? = null,
)

private fun SocketManagerBase.renderNewFile(
  header: String?,
  root: Path,
  ui: ApplicationInterface,
  shouldAutoApply: (Path) -> Boolean,
  codeValue: String,
  handle: (Map<Path, String>) -> Unit,
  codeLang: String
): String {
  val filename = resolve(root, header ?: "Unknown")
  val filepath = root.resolve(filename)
  if (shouldAutoApply(filepath) && !filepath.toFile().exists()) {
    try {
      filepath.parent?.toFile()?.mkdirs()
      filepath.toFile().writeText(codeValue, Charsets.UTF_8)
      handle(mapOf(File(filename).toPath() to codeValue))
      return """
```${codeLang}
${codeValue}
```

<div class="cmd-button">Automatically Saved ${filepath}</div>

"""
    } catch (e: Throwable) {
      return """
```${codeLang}
${codeValue}
```

<div class="cmd-button">Error Auto-Saving ${filename}: ${e.message}</div>
"""
    }
  } else {
    val commandTask = ui.newTask(false)
    lateinit var hrefLink: StringBuilder
    hrefLink = commandTask.complete(hrefLink("Save File", classname = "href-link cmd-button") {
      try {
        filepath.parent?.toFile()?.mkdirs()
        filepath.toFile().writeText(codeValue, Charsets.UTF_8)
        handle(mapOf(File(filename).toPath() to codeValue))
        hrefLink.set("""<div class="cmd-button">Saved ${filepath}</div>""")
        commandTask.complete()
      } catch (e: Throwable) {
        hrefLink.append("""<div class="cmd-button">Error: ${e.message}</div>""")
        commandTask.error(null, e)
      }
    })!!
    return """
```${codeLang}
${codeValue}
```

${commandTask.placeholder}
"""
  }
}

private val pattern_backticks = "`(.*)`".toRegex()


fun resolve(root: Path, filename: String): String {
  var filename = filename.trim()

  filename = if (pattern_backticks.containsMatchIn(filename)) {
    pattern_backticks.find(filename)!!.groupValues[1]
  } else {
    filename
  }

  filename = try {
    val path = File(filename).toPath()
    if (root.contains(path)) path.toString().relativizeFrom(root) else filename
  } catch (e: Throwable) {
    filename
  }

  try {
    if (!root.resolve(filename).toFile().exists()) {
      root.toFile().listFilesRecursively().find { it.toString().replace("\\", "/").endsWith(filename.replace("\\", "/")) }
        ?.toString()?.apply {
          filename = relativizeFrom(root)
        }
    }
  } catch (e: Throwable) {
    log.error("Error resolving filename", e)
  }

  return filename
}

private fun String.relativizeFrom(root: Path) = try {
  root.relativize(File(this).toPath()).toString()
} catch (e: Throwable) {
  this
}

private fun File.listFilesRecursively(): List<File> {
  val files = mutableListOf<File>()
  this.listFiles()?.filter { !isGitignore(it.toPath()) }?.forEach {
    files.add(it.absoluteFile)
    if (it.isDirectory) {
      files.addAll(it.listFilesRecursively())
    }
  }
  return files.toTypedArray().toList()
}

// Function to render a diff block with apply and revert options
private fun SocketManagerBase.renderDiffBlock(
  root: Path,
  filename: String,
  diffVal: String,
  handle: (Map<Path, String>) -> Unit,
  ui: ApplicationInterface,
  api: API?,
  shouldAutoApply: (Path) -> Boolean,
  model: ChatModel? = null,
): String {

  val filepath = root.resolve(filename)
  val prevCode = load(filepath)
  val relativize = try {
    root.relativize(filepath)
  } catch (e: Throwable) {
    filepath
  }
  val applydiffTask = ui.newTask(false)
  lateinit var hrefLink: StringBuilder

  var newCode = patch(prevCode, diffVal)
  val echoDiff = try {
    IterativePatchUtil.generatePatch(prevCode, newCode.newCode)
  } catch (e: Throwable) {
    renderMarkdown("```\n${e.stackTraceToString()}\n```\n", ui = ui)
  }

  // Function to create a revert button
  fun createRevertButton(filepath: Path, originalCode: String, handle: (Map<Path, String>) -> Unit): String {
    val relativize = try {
      root.relativize(filepath)
    } catch (e: Throwable) {
      filepath
    }
    val revertTask = ui.newTask(false)
    lateinit var revertButton: StringBuilder
    revertButton = revertTask.complete(hrefLink("Revert", classname = "href-link cmd-button") {
      try {
        filepath.toFile().writeText(originalCode, Charsets.UTF_8)
        handle(mapOf(relativize to originalCode))
        revertButton.set("""<div class="cmd-button">Reverted</div>""")
        revertTask.complete()
      } catch (e: Throwable) {
        revertButton.append("""<div class="cmd-button">Error: ${e.message}</div>""")
        revertTask.error(null, e)
      }
    })!!
    return revertTask.placeholder
  }

  if (echoDiff.isNotBlank() && newCode.isValid && shouldAutoApply(filepath ?: root.resolve(filename))) {
    try {
      filepath.toFile().writeText(newCode.newCode, Charsets.UTF_8)
      val originalCode = AtomicReference(prevCode)
      handle(mapOf(relativize to newCode.newCode))
      val revertButton = createRevertButton(filepath, originalCode.get(), handle)
      return "```diff\n$diffVal\n```\n" + """<div class="cmd-button">Diff Automatically Applied to ${filepath}</div>""" + revertButton
    } catch (e: Throwable) {
      log.error("Error auto-applying diff", e)
      return "```diff\n$diffVal\n```\n" + """<div class="cmd-button">Error Auto-Applying Diff to ${filepath}: ${e.message}</div>"""
    }
  }


  val diffTask = ui.newTask(root = false)
  diffTask.complete(renderMarkdown("```diff\n$diffVal\n```\n", ui = ui))

  // Create tasks for displaying code and patch information
  val prevCodeTask = ui.newTask(root = false)
  val prevCodeTaskSB = prevCodeTask.add("")
  val newCodeTask = ui.newTask(root = false)
  val newCodeTaskSB = newCodeTask.add("")
  val patchTask = ui.newTask(root = false)
  val patchTaskSB = patchTask.add("")
  val fixTask = ui.newTask(root = false)
  val verifyFwdTabs = if (!newCode.isValid) displayMapInTabs(
    mapOf(
      "Code" to prevCodeTask.placeholder,
      "Preview" to newCodeTask.placeholder,
      "Echo" to patchTask.placeholder,
      "Fix" to fixTask.placeholder,
    )
  ) else displayMapInTabs(
    mapOf(
      "Code" to prevCodeTask.placeholder,
      "Preview" to newCodeTask.placeholder,
      "Echo" to patchTask.placeholder,
    )
  )


  val prevCode2Task = ui.newTask(root = false)
  val prevCode2TaskSB = prevCode2Task.add("")
  val newCode2Task = ui.newTask(root = false)
  val newCode2TaskSB = newCode2Task.add("")
  val patch2Task = ui.newTask(root = false)
  val patch2TaskSB = patch2Task.add("")
  val verifyRevTabs = displayMapInTabs(
    mapOf(
      "Code" to prevCode2Task.placeholder,
      "Preview" to newCode2Task.placeholder,
      "Echo" to patch2Task.placeholder,
    )
  )

  lateinit var revert: String

  var originalCode = prevCode // For reverting changes
  // Create "Apply Diff" button
  val apply1 = hrefLink("Apply Diff", classname = "href-link cmd-button") {
    try {
      originalCode = load(filepath)
      newCode = patch(originalCode, diffVal)
      filepath.toFile().writeText(newCode.newCode, Charsets.UTF_8) ?: log.warn("File not found: $filepath")
      handle(mapOf(relativize to newCode.newCode))
      hrefLink.set("<div class=\"cmd-button\">Diff Applied</div>$revert")
      applydiffTask.complete()
    } catch (e: Throwable) {
      hrefLink.append("""<div class="cmd-button">Error: ${e.message}</div>""")
      applydiffTask.error(null, e)
    }
  }


  if (echoDiff.isNotBlank()) {

    // Add "Fix Patch" button if the patch is not valid
    if (!newCode.isValid) {
      val fixPatchLink = hrefLink("Fix Patch", classname = "href-link cmd-button") {
        try {
          val header = fixTask.header("Attempting to fix patch...")

          val patchFixer = SimpleActor(
            prompt = """
You are a helpful AI that helps people with coding.

Response should use one or more code patches in diff format within ```diff code blocks.
Each diff should be preceded by a header that identifies the file being modified.
The diff format should use + for line additions, - for line deletions.
The diff should include 2 lines of context before and after every change.

Example:

Here are the patches:

### src/utils/exampleUtils.js
```diff
 // Utility functions for example feature
 const b = 2;
 function exampleFunction() {
-   return b + 1;
+   return b + 2;
 }
```

### tests/exampleUtils.test.js
```diff
 // Unit tests for exampleUtils
 const assert = require('assert');
 const { exampleFunction } = require('../src/utils/exampleUtils');
 
 describe('exampleFunction', () => {
-   it('should return 3', () => {
+   it('should return 4', () => {
     assert.equal(exampleFunction(), 3);
   });
 });
```
""",
            model = OpenAIModels.GPT4o,
            temperature = 0.3
          )

          val echoDiff = try {
            IterativePatchUtil.generatePatch(prevCode, newCode.newCode)
          } catch (e: Throwable) {
            renderMarkdown("```\n${e.stackTraceToString()}\n```\n", ui = ui)
          }

          var answer = patchFixer.answer(
            listOf(
              """
Code:
```${filename.split('.').lastOrNull() ?: ""}
$prevCode
```

Patch:
```diff
$diffVal
```

Effective Patch:
```diff
$echoDiff
```

Please provide a fix for the diff above in the form of a diff patch.
"""
            ), api as OpenAIClient
          )
          answer = ui.socketManager?.addApplyFileDiffLinks(root, answer, handle, ui, api, model = model) ?: answer
          header?.clear()
          fixTask.complete(renderMarkdown(answer))
        } catch (e: Throwable) {
          log.error("Error in fix patch", e)
        }
      }
      //apply1 += fixPatchLink
      fixTask.complete(fixPatchLink)
    }

    // Create "Apply Diff (Bottom to Top)" button
    val apply2 = hrefLink("(Bottom to Top)", classname = "href-link cmd-button") {
      try {
        originalCode = load(filepath)
        val originalLines = originalCode.reverseLines()
        val diffLines = diffVal.reverseLines()
        val patch1 = patch(originalLines, diffLines)
        val newCode2 = patch1.newCode.reverseLines()
        filepath.toFile()?.writeText(newCode2, Charsets.UTF_8) ?: log.warn("File not found: $filepath")
        handle(mapOf(relativize to newCode2))
        hrefLink.set("""<div class="cmd-button">Diff Applied (Bottom to Top)</div>""" + revert)
        applydiffTask.complete()
      } catch (e: Throwable) {
        hrefLink.append("""<div class="cmd-button">Error: ${e.message}</div>""")
        applydiffTask.error(null, e)
      }
    }
    // Create "Revert" button
    revert = hrefLink("Revert", classname = "href-link cmd-button") {
      try {
        filepath.toFile()?.writeText(originalCode, Charsets.UTF_8)
        handle(mapOf(relativize to originalCode))
        hrefLink.set("""<div class="cmd-button">Reverted</div>""" + apply1 + apply2)
        applydiffTask.complete()
      } catch (e: Throwable) {
        hrefLink.append("""<div class="cmd-button">Error: ${e.message}</div>""")
        applydiffTask.error(null, e)
      }
    }
    hrefLink = applydiffTask.complete(apply1 + "\n" + apply2)!!
  }

  val lang = filename.split('.').lastOrNull() ?: ""
  newCodeTaskSB?.set(
    renderMarkdown(
      """# $filename

```$lang
${newCode}
```
""",
      ui = ui, tabs = false
    )
  )
  newCodeTask.complete("")
  prevCodeTaskSB?.set(
    renderMarkdown(
      """# $filename

```$lang
${prevCode}
```
""",
      ui = ui, tabs = false
    )
  )
  prevCodeTask.complete("")
  patchTaskSB?.set(
    renderMarkdown(
      """
# $filename

```diff
${echoDiff}
```
""",
      ui = ui,
      tabs = false
    )
  )
  patchTask.complete("")
  val newCode2 = patch(
    load(filepath).reverseLines(),
    diffVal.reverseLines()
  ).newCode.lines().reversed().joinToString("\n")
  val echoDiff2 = try {
    IterativePatchUtil.generatePatch(prevCode, newCode2)
  } catch (e: Throwable) {
    renderMarkdown(
      """

```
${e.stackTraceToString()}
```
""", ui = ui
    )
  }
  newCode2TaskSB?.set(
    renderMarkdown(
      """
# $filename

```${filename.split('.').lastOrNull() ?: ""}
${newCode2}
```
""",
      ui = ui, tabs = false
    )
  )
  newCode2Task.complete("")
  prevCode2TaskSB?.set(
    renderMarkdown(
      """
# $filename

```${filename.split('.').lastOrNull() ?: ""}
${prevCode}
```
""",
      ui = ui, tabs = false
    )
  )
  prevCode2Task.complete("")
  patch2TaskSB?.set(
    renderMarkdown(
      """
# $filename

```diff
  ${echoDiff2}
```
""",
      ui = ui,
      tabs = false
    )
  )
  patch2Task.complete("")


  // Create main tabs for displaying diff and verification information
  val mainTabs = displayMapInTabs(
    mapOf(
      "Diff" to diffTask.placeholder,
      "Verify" to displayMapInTabs(
        mapOf(
          "Forward" to verifyFwdTabs,
          "Reverse" to verifyRevTabs,
        )
      ),
    )
  )
  val newValue = if (newCode.isValid) {
    mainTabs + "\n" + applydiffTask.placeholder
  } else {
    mainTabs + """<div class="warning">Warning: The patch is not valid. Please fix the patch before applying.</div>""" + applydiffTask.placeholder
  }
  return newValue
}

// Function to apply a patch to a code string
private val patch = { code: String, diff: String ->
  val isCurlyBalanced = FileValidationUtils.isCurlyBalanced(code)
  val isSquareBalanced = FileValidationUtils.isSquareBalanced(code)
  val isParenthesisBalanced = FileValidationUtils.isParenthesisBalanced(code)
  val isQuoteBalanced = FileValidationUtils.isQuoteBalanced(code)
  val isSingleQuoteBalanced = FileValidationUtils.isSingleQuoteBalanced(code)
  var newCode = IterativePatchUtil.applyPatch(code, diff)
  newCode = newCode.replace("\r", "")
  val isCurlyBalancedNew = FileValidationUtils.isCurlyBalanced(newCode)
  val isSquareBalancedNew = FileValidationUtils.isSquareBalanced(newCode)
  val isParenthesisBalancedNew = FileValidationUtils.isParenthesisBalanced(newCode)
  val isQuoteBalancedNew = FileValidationUtils.isQuoteBalanced(newCode)
  val isSingleQuoteBalancedNew = FileValidationUtils.isSingleQuoteBalanced(newCode)
  val isError = ((isCurlyBalanced && !isCurlyBalancedNew) ||
      (isSquareBalanced && !isSquareBalancedNew) ||
      (isParenthesisBalanced && !isParenthesisBalancedNew) ||
      (isQuoteBalanced && !isQuoteBalancedNew) ||
      (isSingleQuoteBalanced && !isSingleQuoteBalancedNew))
  PatchResult(newCode, !isError)
}


// Function to load file contents
private fun load(
  filepath: Path?
) = try {
  if (true != filepath?.toFile()?.exists()) {
    log.warn("File not found: $filepath")
    ""
  } else {
    filepath.readText(Charsets.UTF_8)
  }
} catch (e: Throwable) {
  log.error("Error reading file: $filepath", e)
  ""
}

// Function to apply file diffs from a response string
@Suppress("unused")
fun applyFileDiffs(
  root: Path,
  response: String,
): String {
  val headerPattern = """(?s)(?<![^\n])#+\s*([^\n]+)""".toRegex() // capture filename
  val diffPattern = """(?s)(?<![^\n])```diff\n(.*?)\n```""".toRegex() // capture filename
  val codeblockPattern = """(?s)(?<![^\n])```([^\n])(\n.*?\n)```""".toRegex() // capture filename
  val headers = headerPattern.findAll(response).map { it.range to it.groupValues[1] }.toList()
  val diffs: List<Pair<IntRange, String>> =
    diffPattern.findAll(response).map { it.range to it.groupValues[1] }.toList()
  val codeblocks = codeblockPattern.findAll(response).filter {
    when (it.groupValues[1]) {
      "diff" -> false
      else -> true
    }
  }.map { it.range to it }.toList()
  diffs.forEach { diffBlock ->
    val header = headers.lastOrNull { it.first.last < diffBlock.first.first }
    val filename = resolve(root, header?.second ?: "Unknown")
    val diffVal = diffBlock.second
    val filepath = root.resolve(filename)
    try {
      val originalCode = filepath.readText(Charsets.UTF_8)
      val newCode = patch(originalCode, diffVal)
      filepath.toFile().writeText(newCode.newCode, Charsets.UTF_8)
    } catch (e: Throwable) {
      log.warn("Error", e)
    }
  }
  codeblocks.forEach { codeBlock ->
    val header = headers.lastOrNull { it.first.last < codeBlock.first.first }
    val filename = resolve(root, header?.second ?: "Unknown")
    val filepath: Path? = root.resolve(filename)
    val codeValue = codeBlock.second.groupValues[2].trim()
    lateinit var hrefLink: StringBuilder
    try {
      try {
        filepath?.toFile()?.writeText(codeValue, Charsets.UTF_8)
      } catch (e: Throwable) {
        log.error("Error writing file: $filepath", e)
      }
      hrefLink.set("""<div class="cmd-button">Saved ${filename}</div>""")
    } catch (e: Throwable) {
      log.error("Error", e)
    }
  }
  return response
}


private val log = org.slf4j.LoggerFactory.getLogger(PatchUtil::class.java)