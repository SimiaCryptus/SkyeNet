package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.skyenet.Retryable
import com.simiacryptus.skyenet.apps.plan.DocumentationTask.DocumentationTaskData
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.util.MarkdownUtil
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.util.JsonUtil
import org.slf4j.LoggerFactory
import java.util.concurrent.Semaphore

class DocumentationTask(
    planSettings: PlanSettings,
    planTask: DocumentationTaskData?
) : AbstractTask<DocumentationTaskData>(planSettings, planTask) {
    class DocumentationTaskData(
        @Description("List of files or tasks to be documented")
        val items_to_document: List<String>? = null,
        task_description: String? = null,
        task_dependencies: List<String>? = null,
        input_files: List<String>? = null,
        output_files: List<String>? = null,
        state: TaskState? = null
    ) : PlanTaskBase(
        task_type = TaskType.Documentation.name,
        task_description = task_description,
        task_dependencies = task_dependencies,
        input_files = input_files,
        output_files = output_files,
        state = state
    )

    override fun promptSegment(): String {
        return """
 Documentation - Generate documentation
   ** List input files/tasks to be examined
            """.trimMargin()
    }

    val documentationGeneratorActor by lazy {
        SimpleActor(
            name = "DocumentationGenerator",
            prompt = """
 Create detailed and clear documentation for the provided code, covering its purpose, functionality, inputs, outputs, and any assumptions or limitations.
 Use a structured and consistent format that facilitates easy understanding and navigation. 
 Include code examples where applicable, and explain the rationale behind key design decisions and algorithm choices.
 Document any known issues or areas for improvement, providing guidance for future developers on how to extend or maintain the code.
                """.trimMargin(),
            model = planSettings.getTaskSettings(TaskType.Documentation).model ?: planSettings.defaultModel,
            temperature = planSettings.temperature,
        )
    }

    override fun run(
        agent: PlanCoordinator,
        taskId: String,
        userMessage: String,
        plan: Map<String, PlanTaskBase>,
        planProcessingState: PlanProcessingState,
        task: SessionTask,
        api: API
    ) {
        val semaphore = Semaphore(0)
        val onComplete = {
            semaphore.release()
        }
        val process = { sb: StringBuilder ->
            val itemsToDocument = planTask?.items_to_document ?: emptyList()
            val docResult = documentationGeneratorActor.answer(
                listOf<String>(
                    userMessage,
                    JsonUtil.toJson(plan),
                    getPriorCode(planProcessingState),
                    getInputFileCode(),
                    "Items to document: ${itemsToDocument.joinToString(", ")}"
                ).filter { it.isNotBlank() }, api
            )
            planProcessingState.taskResult[taskId] = docResult
            if (agent.planSettings.autoFix) {
                task.complete()
                onComplete()
                MarkdownUtil.renderMarkdown("## Generated Documentation\n$docResult\nAuto-accepted", ui = agent.ui)
            } else {
                MarkdownUtil.renderMarkdown(
                    "## Generated Documentation\n$docResult",
                    ui = agent.ui
                ) + acceptButtonFooter(agent.ui) {
                    task.complete()
                    onComplete()
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
        private val log = LoggerFactory.getLogger(DocumentationTask::class.java)
    }
}