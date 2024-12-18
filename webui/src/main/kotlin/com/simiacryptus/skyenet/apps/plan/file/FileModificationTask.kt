package com.simiacryptus.skyenet.apps.plan.file

import com.simiacryptus.diff.addApplyFileDiffLinks
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.skyenet.Retryable
import com.simiacryptus.skyenet.apps.plan.*
import com.simiacryptus.skyenet.apps.plan.file.FileModificationTask.FileModificationTaskConfigData
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import com.simiacryptus.skyenet.webui.session.SessionTask
import org.slf4j.LoggerFactory
import java.util.concurrent.Semaphore

class FileModificationTask(
  planSettings: PlanSettings,
  planTask: FileModificationTaskConfigData?
) : AbstractFileTask<FileModificationTaskConfigData>(planSettings, planTask) {
  class FileModificationTaskConfigData(
    input_files: List<String>? = null,
    output_files: List<String>? = null,
    @Description("Specific modifications to be made to the files")
    val modifications: Any? = null,
    task_description: String? = null,
    task_dependencies: List<String>? = null,
    state: TaskState? = null
  ) : FileTaskConfigBase(
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
    messages: List<String>,
    task: SessionTask,
    api: ChatClient,
    resultFn: (String) -> Unit,
    api2: OpenAIClient,
    planSettings: PlanSettings
  ) {
    val defaultFile = if (((planTask?.input_files ?: listOf()) + (planTask?.output_files ?: listOf())).isEmpty()) {
      task.complete("CONFIGURATION ERROR: No input files specified")
      resultFn("CONFIGURATION ERROR: No input files specified")
      return
    } else if(((planTask?.input_files ?: listOf()) + (planTask?.output_files ?: listOf())).distinct().size == 1) {
      ((planTask?.input_files ?: listOf()) + (planTask?.output_files ?: listOf())).first()
    } else {
      null
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
        task.complete()
        onComplete()
        renderMarkdown(codeResult, ui = agent.ui) {
          agent.ui.socketManager!!.addApplyFileDiffLinks(
            root = agent.root,
            response = it,
            handle = { newCodeMap ->
              newCodeMap.forEach { (path, newCode) ->
                task.complete("<a href='${"fileIndex/${agent.session}/$path"}'>$path</a> Updated")
              }
            },
            ui = agent.ui,
            api = api,
            shouldAutoApply = { agent.planSettings.autoFix },
            model = planSettings.getTaskSettings(TaskType.FileModification).model ?: planSettings.defaultModel,
            defaultFile = defaultFile
          ) + "\n\n## Auto-applied changes"
        }
      } else {
        renderMarkdown(codeResult, ui = agent.ui) {
          agent.ui.socketManager!!.addApplyFileDiffLinks(
            root = agent.root,
            response = it,
            handle = { newCodeMap ->
              newCodeMap.forEach { (path, newCode) ->
                task.complete("<a href='${"fileIndex/${agent.session}/$path"}'>$path</a> Updated")
              }
            },
            ui = agent.ui,
            api = api,
            model = planSettings.getTaskSettings(TaskType.FileModification).model ?: planSettings.defaultModel,
            defaultFile = defaultFile,
          ) + acceptButtonFooter(agent.ui) {
            task.complete()
            onComplete()
          }
        }
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