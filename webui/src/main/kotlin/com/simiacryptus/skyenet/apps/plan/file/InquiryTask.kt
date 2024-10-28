package com.simiacryptus.skyenet.apps.plan.file

import com.simiacryptus.diff.FileValidationUtils
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.models.ApiModel
import com.simiacryptus.jopenai.models.ApiModel.Role
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.skyenet.Discussable
import com.simiacryptus.skyenet.apps.plan.*
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.util.MarkdownUtil
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.util.JsonUtil
import org.slf4j.LoggerFactory
import java.nio.file.FileSystems
import java.nio.file.Files
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference
import kotlin.streams.asSequence

class InquiryTask(
    planSettings: PlanSettings,
    planTask: InquiryTaskData?
) : AbstractTask<InquiryTask.InquiryTaskData>(planSettings, planTask) {
    class InquiryTaskData(
        @Description("The specific questions or topics to be addressed in the inquiry")
        val inquiry_questions: List<String>? = null,
        @Description("The goal or purpose of the inquiry")
        val inquiry_goal: String? = null,
        @Description("The specific files (or file patterns) to be used as input for the task")
        val input_files: List<String>? = null,
        task_description: String? = null,
        task_dependencies: List<String>? = null,
        state: TaskState? = null,
    ) : PlanTaskBase(
        task_type = TaskType.Inquiry.name,
        task_description = task_description,
        task_dependencies = task_dependencies,
        state = state
    )

    override fun promptSegment() = if (planSettings.allowBlocking) """
    |Inquiry - Answer questions by reading in files and providing a summary that can be discussed with and approved by the user
    |    ** Specify the questions and the goal of the inquiry
    |    ** List input files to be examined when answering the questions
    """.trimMargin() else """
    |Inquiry - Answer questions by reading in files and providing a report
    |    ** Specify the questions and the goal of the inquiry
    |    ** List input files to be examined when answering the questions
    """.trimMargin()

    private val inquiryActor by lazy {
        SimpleActor(
            name = "Inquiry",
            prompt = """
                Create code for a new file that fulfills the specified requirements and context.
                Given a detailed user request, break it down into smaller, actionable tasks suitable for software development.
                Compile comprehensive information and insights on the specified topic.
                Provide a comprehensive overview, including key concepts, relevant technologies, best practices, and any potential challenges or considerations. 
                Ensure the information is accurate, up-to-date, and well-organized to facilitate easy understanding.
                
                When generating insights, consider the existing project context and focus on information that is directly relevant and applicable.
                Focus on generating insights and information that support the task types available in the system (${
                planSettings.taskSettings.filter { it.value.enabled }.keys.joinToString(", ")
            }).
                This will ensure that the inquiries are tailored to assist in the planning and execution of tasks within the system's framework.
                """.trimMargin(),
            model = planSettings.getTaskSettings(TaskType.valueOf(planTask?.task_type!!)).model ?: planSettings.defaultModel,
            temperature = planSettings.temperature,
        )
    }

    override fun run(
        agent: PlanCoordinator,
        messages: List<String>,
        task: SessionTask,
        api: API,
        resultFn: (String) -> Unit
    ) {

        val toInput = { it: String ->
            messages + listOf<String>(
                getInputFileCode(),
                it,
            ).filter { it.isNotBlank() }
        }

        val inquiryResult = if (planSettings.allowBlocking) Discussable(
            task = task,
            userMessage = {
                "Expand ${this.planTask?.task_description ?: ""}\nQuestions: ${
                    planTask?.inquiry_questions?.joinToString(
                        "\n"
                    )
                }\nGoal: ${planTask?.inquiry_goal}\n${JsonUtil.toJson(data = this)}"
            },
            heading = "",
            initialResponse = { it: String -> inquiryActor.answer(toInput(it), api = api) },
            outputFn = { design: String ->
                MarkdownUtil.renderMarkdown(design, ui = agent.ui)
            },
            ui = agent.ui,
            reviseResponse = { usermessages: List<Pair<String, Role>> ->
                val inStr = "Expand ${this.planTask?.task_description ?: ""}\nQuestions: ${
                    planTask?.inquiry_questions?.joinToString("\n")
                }\nGoal: ${planTask?.inquiry_goal}\n${JsonUtil.toJson(data = this)}"
                val messages = usermessages.map { ApiModel.ChatMessage(it.second, it.first.toContentList()) }
                    .toTypedArray<ApiModel.ChatMessage>()
                inquiryActor.respond(
                    messages = messages,
                    input = toInput(inStr),
                    api = api
                )
            },
            atomicRef = AtomicReference(),
            semaphore = Semaphore(0),
        ).call() else inquiryActor.answer(
            toInput(
                "Expand ${this.planTask?.task_description ?: ""}\nQuestions: ${
                    planTask?.inquiry_questions?.joinToString(
                        "\n"
                    )
                }\nGoal: ${planTask?.inquiry_goal}\n${JsonUtil.toJson(data = this)}"
            ),
            api = api
        ).apply {
            task.add(MarkdownUtil.renderMarkdown(this, ui = agent.ui))
        }
        resultFn(inquiryResult)
    }

    private fun getInputFileCode(): String =
        ((planTask?.input_files ?: listOf()))
            .flatMap { pattern: String ->
                val matcher = FileSystems.getDefault().getPathMatcher("glob:$pattern")
                Files.walk(root).asSequence()
                    .filter { path ->
                        matcher.matches(root.relativize(path)) &&
                                FileValidationUtils.isLLMIncludableFile(path.toFile())
                    }
                    .map { path ->
                        root.relativize(path).toString()
                    }
                    .toList()
            }
            .distinct()
            .sortedBy { it }
            .joinToString("\n\n") { relativePath ->
                val file = root.resolve(relativePath).toFile()
                try {
                    """
                |# $relativePath
                |
                |${AbstractFileTask.TRIPLE_TILDE}
                |${codeFiles[file.toPath()] ?: file.readText()}
                |${AbstractFileTask.TRIPLE_TILDE}
                """.trimMargin()
                } catch (e: Throwable) {
                    log.warn("Error reading file: $relativePath", e)
                    ""
                }
            }

    companion object {
        private val log = LoggerFactory.getLogger(InquiryTask::class.java)
    }
}