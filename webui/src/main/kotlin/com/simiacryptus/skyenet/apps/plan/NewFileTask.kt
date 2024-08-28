package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.diff.addApplyFileDiffLinks
import com.simiacryptus.skyenet.Retryable
import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.core.actors.ParsedResponse
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.util.MarkdownUtil
import org.slf4j.LoggerFactory
import java.util.concurrent.Semaphore

class NewFileTask(
    settings: Settings,
    task: PlanCoordinator.Task
) : AbstractTask(settings, task) {
    val newFileCreatorActor by lazy {
        SimpleActor(
            name = "NewFileCreator",
            prompt = """
         Generate the necessary code for new files based on the given requirements and context.
         For each file:
         Provide a clear relative file path based on the content and purpose of the file.
         Ensure the code is well-structured, follows best practices, and meets the specified functionality.
         Carefully consider how the new file fits into the existing project structure and architecture.
         Avoid creating files that duplicate functionality or introduce inconsistencies.
                |  
                |The response format should be as follows:
                |- Use triple backticks to create code blocks for each file.
                |- Each code block should be preceded by a header specifying the file path.
                |- The file path should be a relative path from the project root.
                |- Separate code blocks with a single blank line.
                |- Specify the language for syntax highlighting after the opening triple backticks.
                |
                |Example:
                |
                |Here are the new files:
                |
                |### src/utils/exampleUtils.js
                |${TRIPLE_TILDE}js
                |// Utility functions for example feature
                |const b = 2;
                |function exampleFunction() {
                |  return b + 1;
                |}
                |
                |$TRIPLE_TILDE
                |
                |### tests/exampleUtils.test.js 
                |${TRIPLE_TILDE}js
                |// Unit tests for exampleUtils
                |const assert = require('assert');
                |const { exampleFunction } = require('../src/utils/exampleUtils');
                |
                |describe('exampleFunction', () => {
                |  it('should return 3', () => {
                |    assert.equal(exampleFunction(), 3);
                |  });
                |});
         $TRIPLE_TILDE
              """.trimMargin(),
            model = settings.model,
            temperature = settings.temperature,
        )
    }

    override fun promptSegment(): String {
        return """
            NewFile - Create one or more new files, carefully considering how they fit into the existing project structure
              ** For each file, specify the relative file path and the purpose of the file
              ** List input files/tasks to be examined when authoring the new files
        """.trimIndent()
    }

    override fun run(
        agent: PlanCoordinator,
        taskId: String,
        userMessage: String,
        plan: ParsedResponse<PlanCoordinator.TaskBreakdownResult>,
        genState: PlanCoordinator.GenState,
        task: SessionTask,
        taskTabs: TabbedDisplay
    ) {
        val semaphore = Semaphore(0)
        val onComplete = { semaphore.release() }
        val process = { sb: StringBuilder ->
            val codeResult = newFileCreatorActor.answer(
                listOf<String>(
                    userMessage,
                    plan.text,
                    getPriorCode(genState),
                    getInputFileCode(),
                    this.description ?: "",
                ).filter { it.isNotBlank() }, agent.api
            )
            genState.taskResult[taskId] = codeResult
            if (agent.settings.autoFix) {
                val diffLinks = agent.ui.socketManager!!.addApplyFileDiffLinks(
                    agent.root,
                    codeResult,
                    api = agent.api,
                    ui = agent.ui,
                    shouldAutoApply = { true })
                taskTabs.selectedTab += 1
                taskTabs.update()
                onComplete()
                MarkdownUtil.renderMarkdown(diffLinks + "\n\n## Auto-applied changes", ui = agent.ui)
            } else {
                MarkdownUtil.renderMarkdown(
                    agent.ui.socketManager!!.addApplyFileDiffLinks(
                        agent.root,
                        codeResult,
                        api = agent.api,
                        ui = agent.ui
                    ),
                    ui = agent.ui
                ) + acceptButtonFooter(agent.ui) {
                    taskTabs.selectedTab += 1
                    taskTabs.update()
                    onComplete()
                }
            }
        }
        Retryable(
            agent.ui, task = task,
            process = process
        )
        try {
            semaphore.acquire()
        } catch (e: Throwable) {
            PlanCoordinator.log.warn("Error", e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(NewFileTask::class.java)
    }
}