package com.simiacryptus.skyenet.apps.plan.file

import com.simiacryptus.diff.addApplyFileDiffLinks
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.skyenet.Retryable
import com.simiacryptus.skyenet.apps.plan.*
import com.simiacryptus.skyenet.apps.plan.file.FileModificationTask.FileModificationTaskData
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.util.MarkdownUtil
import com.simiacryptus.skyenet.webui.session.SessionTask
import org.slf4j.LoggerFactory
import java.util.concurrent.Semaphore

class FileModificationTask(
    planSettings: PlanSettings,
    planTask: FileModificationTaskData?
) : AbstractFileTask<FileModificationTaskData>(planSettings, planTask) {
    class FileModificationTaskData(
        @Description("List of input files to be examined when designing the modifications")
        input_files: List<String>? = null,
        @Description("List of output files to be modified or created")
        output_files: List<String>? = null,
        @Description("Specific modifications to be made to the files")
        val modifications: Map<String, String>? = null,
        task_description: String? = null,
        task_dependencies: List<String>? = null,
        state: TaskState? = null
    ) : FileTaskBase(
        task_type = TaskType.FileModification.name,
        task_description = task_description,
        task_dependencies = task_dependencies,
        input_files = input_files,
        output_files = output_files,
        state = state
    )

    val fileModificationActor by lazy {
        SimpleActor(
            name = "FileModification",
            prompt = """
 Generate patches for existing files or create new files based on the given requirements and context.
 For existing files:
 Ensure modifications are efficient, maintain readability, and adhere to coding standards.
 Carefully review the existing code and project structure to ensure changes are consistent and do not introduce bugs.
 Consider the impact of modifications on other parts of the codebase.
                
 For new files:
 Provide a clear relative file path based on the content and purpose of the file.
 Ensure the code is well-structured, follows best practices, and meets the specified functionality.
 Carefully consider how the new file fits into the existing project structure and architecture.
 Avoid creating files that duplicate functionality or introduce inconsistencies.
                
 Provide a summary of the changes made.
                
 Response format:
 For existing files: Use ${TRIPLE_TILDE}diff code blocks with a header specifying the file path.
 For new files: Use $TRIPLE_TILDE code blocks with a header specifying the new file path.
 The diff format should use + for line additions, - for line deletions.
 Include 2 lines of context before and after every change in diffs.
 Separate code blocks with a single blank line.
 For new files, specify the language for syntax highlighting after the opening triple backticks.
                
 Example:
                
 Here are the modifications:
                
 ### src/utils/existingFile.js
 ${TRIPLE_TILDE}diff
  // Existing utility functions
  function existingFunction() {
  return 'old result';
  return 'new result';
                | }
 $TRIPLE_TILDE
                
 ### src/utils/newFile.js
 ${TRIPLE_TILDE}js
 // New utility functions
 function newFunction() {
   return 'new functionality';
                |}
 $TRIPLE_TILDE
                """.trimMargin(),
            model = planSettings.getTaskSettings(TaskType.FileModification).model ?: planSettings.defaultModel,
            temperature = planSettings.temperature,
        )
    }

    override fun promptSegment(): String {
        return """
 FileModification - Modify existing files or create new files
   ** For each file, specify the relative file path and the goal of the modification or creation
   ** List input files/tasks to be examined when designing the modifications or new files
        """.trimMargin()
    }

    override fun run(
        agent: PlanCoordinator,
        taskId: String,
        messages: List<String>,
        task: SessionTask,
        api: API,
        resultFn: (String) -> Unit
    ) {
        if (((planTask?.input_files ?: listOf()) + (planTask?.output_files ?: listOf())).isEmpty()) {
            task.complete("No input files specified")
            return
        }
        val semaphore = Semaphore(0)
        val onComplete = { semaphore.release() }
        val process = { sb: StringBuilder ->
            val codeResult = fileModificationActor.answer(
                (messages + listOf(
                    getInputFileCode(),
                    this.planTask?.task_description ?: "",
                )).filter { it.isNotBlank() }, api
            )
            resultFn(codeResult)
            if (agent.planSettings.autoFix) {
                val diffLinks = agent.ui.socketManager!!.addApplyFileDiffLinks(
                    root = agent.root,
                    response = codeResult,
                    handle = { newCodeMap ->
                        newCodeMap.forEach { (path, newCode) ->
                            task.complete("<a href='${"fileIndex/${agent.session}/$path"}'>$path</a> Updated")
                        }
                    },
                    ui = agent.ui,
                    api = api,
                    shouldAutoApply = { agent.planSettings.autoFix }
                )
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
                        api = api
                    ) + acceptButtonFooter(agent.ui) {
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
            log.warn("Error", e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(FileModificationTask::class.java)
    }
}