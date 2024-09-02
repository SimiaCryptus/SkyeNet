package com.simiacryptus.skyenet.apps.general

import com.simiacryptus.diff.FileValidationUtils
import com.simiacryptus.diff.addApplyFileDiffLinks
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.models.OpenAITextModel
import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.AgentPatterns
import com.simiacryptus.skyenet.Retryable
import com.simiacryptus.skyenet.core.actors.ParsedActor
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationSocketManager
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.session.SocketManager
import com.simiacryptus.skyenet.webui.util.MarkdownUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path

abstract class PatchApp(
    override val root: File,
    val session: Session,
    val settings: Settings,
    val api: OpenAIClient,
    val model: OpenAITextModel
) : ApplicationServer(
    applicationName = "Magic Code Fixer",
    path = "/fixCmd",
    showMenubar = false,
) {
    companion object {
        private val log = LoggerFactory.getLogger(PatchApp::class.java)
        const val tripleTilde = "`" + "``" // This is a workaround for the markdown parser when editing this file
    }

    data class OutputResult(val exitCode: Int, val output: String)

    abstract fun codeFiles(): Set<Path>
    abstract fun codeSummary(paths: List<Path>): String
    abstract fun output(task: SessionTask): OutputResult
    abstract fun searchFiles(searchStrings: List<String>): Set<Path>
    override val singleInput = true
    override val stickyInput = false
    override fun newSession(user: User?, session: Session ): SocketManager {
        val socketManager = super.newSession(user, session)
        val ui = (socketManager as ApplicationSocketManager).applicationInterface
        val task = ui.newTask()
        Retryable(
            ui = ui,
            task = task,
            process = { content ->
                val newTask = ui.newTask(false)
                newTask.add("Running Command")
                Thread {
                    run(ui, newTask)
                }.start()
                newTask.placeholder
            }
        )
        return socketManager
    }

    abstract fun projectSummary(): String

    private fun prunePaths(paths: List<Path>, maxSize: Int): List<Path> {
        val sortedPaths = paths.sortedByDescending { it.toFile().length() }
        var totalSize = 0
        val prunedPaths = mutableListOf<Path>()
        for (path in sortedPaths) {
            val fileSize = path.toFile().length().toInt()
            if (totalSize + fileSize > maxSize) break
            prunedPaths.add(path)
            totalSize += fileSize
        }
        return prunedPaths
    }

    data class ParsedErrors(
        val errors: List<ParsedError>? = null
    )

    data class ParsedError(
        @Description("The error message")
        val message: String? = null,
        @Description("Files identified as needing modification and issue-related files")
        val relatedFiles: List<String>? = null,
        @Description("Files identified as needing modification and issue-related files")
        val fixFiles: List<String>? = null,
        @Description("Search strings to find relevant files")
        val searchStrings: List<String>? = null
    )

    data class Settings(
        var executable: File,
        var arguments: String = "",
        var workingDirectory: File? = null,
        var exitCodeOption: String = "nonzero",
        var additionalInstructions: String = "",
        val autoFix: Boolean,
    )

    fun run(
        ui: ApplicationInterface,
        task: SessionTask,
    ): OutputResult {
        val output = output(task)
        if (output.exitCode == 0 && settings.exitCodeOption == "nonzero") {
            task.complete(
                """
                |<div>
                |<div><b>Command executed successfully</b></div>
                |${MarkdownUtil.renderMarkdown("${tripleTilde}\n${output.output}\n${tripleTilde}")}
                |</div>
                |""".trimMargin()
            )
            return output
        }
        if (settings.exitCodeOption == "zero" && output.exitCode != 0) {
            task.complete(
                """
                |<div>
                |<div><b>Command failed</b></div>
                |${MarkdownUtil.renderMarkdown("${tripleTilde}\n${output.output}\n${tripleTilde}")}
                |</div>
                |""".trimMargin()
            )
            return output
        }
        try {
            task.add(
                """
                |<div>
                |<div><b>Command exit code: ${output.exitCode}</b></div>
                |${MarkdownUtil.renderMarkdown("${tripleTilde}\n${output.output}\n${tripleTilde}")}
                |</div>
                """.trimMargin()
            )
            fixAll(settings, output, task, ui, api)
        } catch (e: Exception) {
            task.error(ui, e)
        }
        return output
    }

    private fun fixAll(
        settings: Settings,
        output: OutputResult,
        task: SessionTask,
        ui: ApplicationInterface,
        api: OpenAIClient,
    ) {
        Retryable(ui, task) { content ->
            fixAllInternal(
                settings = settings,
                output = output,
                task = task,
                ui = ui,
                changed = mutableSetOf(),
                api = api
            )
            content.clear()
            ""
        }
    }

    private fun fixAllInternal(
        settings: Settings,
        output: OutputResult,
        task: SessionTask,
        ui: ApplicationInterface,
        changed: MutableSet<Path>,
        api: OpenAIClient,
    ) {
        val plan = ParsedActor(
            resultClass = ParsedErrors::class.java,
            prompt = """
                |You are a helpful AI that helps people with coding.
                |
                |You will be answering questions about the following project:
                |
                |Project Root: ${settings.workingDirectory?.absolutePath ?: ""}
                |
                |Files:
                |${projectSummary()}
                |
                |Given the response of a build/test process, identify one or more distinct errors.
                |For each error:
                |   1) predict the files that need to be fixed
                |   2) predict related files that may be needed to debug the issue
                |   3) specify a search string to find relevant files - be as specific as possible
                |${if (settings.additionalInstructions.isNotBlank()) "Additional Instructions:\n  ${settings.additionalInstructions}\n" else ""}
                """.trimMargin(),
            model = model
        ).answer(
            listOf(
                """
                |The following command was run and produced an error:
                |
                |${tripleTilde}
                |${output.output}
                |${tripleTilde}
                """.trimMargin()
            ), api = api
        )
        task.add(
            AgentPatterns.displayMapInTabs(
                mapOf(
                    "Text" to MarkdownUtil.renderMarkdown(plan.text, ui = ui),
                    "JSON" to MarkdownUtil.renderMarkdown(
                        "${tripleTilde}json\n${JsonUtil.toJson(plan.obj)}\n${tripleTilde}",
                        ui = ui
                    ),
                )
            )
        )
        val progressHeader = task.header("Processing tasks")
        plan.obj.errors?.forEach { error ->
            task.header("Processing error: ${error.message}")
            task.verbose(MarkdownUtil.renderMarkdown("```json\n${JsonUtil.toJson(error)}\n```", tabs = false, ui = ui))
            // Search for files using the provided search strings
            val searchResults = error.searchStrings?.flatMap { searchString ->
                FileValidationUtils.filteredWalk(settings.workingDirectory!!) { !FileValidationUtils.isGitignore(it.toPath()) }
                    .filter { FileValidationUtils.isLLMIncludable(it) }
                    .filter { it.readText().contains(searchString, ignoreCase = true) }
                    .map { it.toPath() }
                    .toList()
            }?.toSet() ?: emptySet()
            task.verbose(
                MarkdownUtil.renderMarkdown(
                    """
                    |Search results:
                    |
                    |${searchResults.joinToString("\n") { "* `$it`" }}
                    """.trimMargin(), tabs = false, ui = ui
                )
            )
            Retryable(ui, task) { content ->
                fix(
                    error, searchResults.toList().map { it.toFile().absolutePath },
                    output, ui, content, settings.autoFix, changed, api
                )
                content.toString()
            }
        }
        progressHeader?.clear()
        task.append("", false)
    }

    private fun fix(
        error: ParsedError,
        additionalFiles: List<String>? = null,
        output: OutputResult,
        ui: ApplicationInterface,
        content: StringBuilder,
        autoFix: Boolean,
        changed: MutableSet<Path>,
        api: OpenAIClient,
    ) {
        val paths =
            (
                    (error.fixFiles ?: emptyList()) +
                            (error.relatedFiles ?: emptyList()) +
                            (additionalFiles ?: emptyList())
                    ).map {
                    try {
                        File(it).toPath()
                    } catch (e: Throwable) {
                        log.warn("Error: root=${root}    ", e)
                        null
                    }
                }.filterNotNull()
        val prunedPaths = prunePaths(paths, 50 * 1024)
        val summary = codeSummary(prunedPaths)
        val response = SimpleActor(
            prompt = """
                    |You are a helpful AI that helps people with coding.
                    |
                    |You will be answering questions about the following code:
                    |
                    |$summary
                    |
                    |
                    |Response should use one or more code patches in diff format within ${tripleTilde}diff code blocks.
                    |Each diff should be preceded by a header that identifies the file being modified.
                    |The diff format should use + for line additions, - for line deletions.
                    |The diff should include 2 lines of context before and after every change.
                    |
                    |Example:
                    |
                    |Here are the patches:
                    |
                    |### src/utils/exampleUtils.js
                    |${tripleTilde}diff
                    | // Utility functions for example feature
                    | const b = 2;
                    | function exampleFunction() {
                    |-   return b + 1;
                    |+   return b + 2;
                    | }
                    |${tripleTilde}
                    |
                    |### tests/exampleUtils.test.js
                    |${tripleTilde}diff
                    | // Unit tests for exampleUtils
                    | const assert = require('assert');
                    | const { exampleFunction } = require('../src/utils/exampleUtils');
                    | 
                    | describe('exampleFunction', () => {
                    |-   it('should return 3', () => {
                    |+   it('should return 4', () => {
                    |     assert.equal(exampleFunction(), 3);
                    |   });
                    | });
                    |${tripleTilde}
                    |
                    |If needed, new files can be created by using code blocks labeled with the filename in the same manner.
                    """.trimMargin(),
            model = model
        ).answer(
            listOf(
                """
                |The following command was run and produced an error:
                |
                |${tripleTilde}
                |${output.output}
                |${tripleTilde}
                |
                |Focus on and Fix the Error:
                |  ${error.message?.replace("\n", "\n  ") ?: ""}
                |${if (settings.additionalInstructions.isNotBlank()) "Additional Instructions:\n  ${settings.additionalInstructions}\n" else ""}
                """.trimMargin()
            ), api = api
        )
        var markdown = ui.socketManager?.addApplyFileDiffLinks(
            root = root.toPath(),
            response = response,
            ui = ui,
            api = api,
            shouldAutoApply = { path ->
                if (autoFix && !changed.contains(path)) {
                    changed.add(path)
                    true
                } else {
                    false
                }
            }
        )
        content.clear()
        content.append("<div>${MarkdownUtil.renderMarkdown(markdown!!)}</div>")
    }

}



