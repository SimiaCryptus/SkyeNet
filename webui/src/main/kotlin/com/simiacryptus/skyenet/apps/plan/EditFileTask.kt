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

class EditFileTask(
    settings: Settings,
    task: PlanCoordinator.Task
) : AbstractTask(settings, task) {
    val filePatcherActor by lazy {
        SimpleActor(
            name = "FilePatcher",
            prompt = """
                |Generate a patch for an existing file to modify its functionality or fix issues based on the given requirements and context. 
                |Ensure the modifications are efficient, maintain readability, and adhere to coding standards.
                |Carefully review the existing code and project structure to ensure the changes are consistent and do not introduce bugs.
                |Consider the impact of the modifications on other parts of the codebase.
                |
                |Provide a summary of the changes made.
                |  
                |Response should use one or more code patches in diff format within ${TRIPLE_TILDE}diff code blocks.
                |Each diff should be preceded by a header that identifies the file being modified.
                |The diff format should use + for line additions, - for line deletions.
                |The diff should include 2 lines of context before and after every change.
                |
                |Example:
                |
                |Here are the patches:
                |
                |### src/utils/exampleUtils.js
                |${TRIPLE_TILDE}diff
                | // Utility functions for example feature
                | const b = 2;
                | function exampleFunction() {
                |-   return b + 1;
                |+   return b + 2;
                | }
                |$TRIPLE_TILDE
                |
                |### tests/exampleUtils.test.js
                |${TRIPLE_TILDE}diff
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
                |$TRIPLE_TILDE
                """.trimMargin(),
            model = settings.model,
            temperature = settings.temperature,
        )
    }

    override fun promptSegment(): String {
        return """
            |EditFile - Modify existing files
            |  ** For each file, specify the relative file path and the goal of the modification
            |  ** List input files/tasks to be examined when designing the modifications
            """.trimMargin()
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
            val codeResult = filePatcherActor.answer(
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
                    root = agent.root,
                    response = codeResult,
                    handle = { newCodeMap ->
                        newCodeMap.forEach { (path, newCode) ->
                            task.complete("<a href='${"fileIndex/${agent.session}/$path"}'>$path</a> Updated")
                        }
                    },
                    ui = agent.ui,
                    api = agent.api,
                    shouldAutoApply = { true }
                )
                taskTabs.selectedTab += 1
                taskTabs.update()
                task.complete()
                onComplete()
                MarkdownUtil.renderMarkdown(diffLinks + "\n\n## Auto-applied changes", ui = agent.ui)
            } else {
                MarkdownUtil.renderMarkdown(
                    agent.ui.socketManager!!.addApplyFileDiffLinks(
                        root = agent.root,
                        response = codeResult,
                        handle = { newCodeMap ->
                            newCodeMap.forEach { (path, newCode) ->
                                task.complete("<a href='${"fileIndex/${agent.session}/$path"}'>$path</a> Updated")
                            }
                        },
                        ui = agent.ui,
                        api = agent.api
                    ) + acceptButtonFooter(agent.ui) {
                        taskTabs.selectedTab += 1
                        taskTabs.update()
                        task.complete()
                        onComplete()
                    }, ui = agent.ui
                )
            }
        }
        Retryable(agent.ui, task = task, process = process)
        try {
            semaphore.acquire()
        } catch (e: Throwable) {
            PlanCoordinator.log.warn("Error", e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(EditFileTask::class.java)
    }
}