package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.models.ApiModel
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.skyenet.Discussable
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.util.MarkdownUtil
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.util.JsonUtil
import org.slf4j.LoggerFactory
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference

class InquiryTask(
    planSettings: PlanSettings,
    planTask: InquiryTaskData?
) : AbstractTask<InquiryTask.InquiryTaskData>(planSettings, planTask) {
    class InquiryTaskData(
        @Description("The specific questions or topics to be addressed in the inquiry")
        val inquiry_questions: List<String>? = null,
        @Description("The goal or purpose of the inquiry")
        val inquiry_goal: String? = null,
        task_type: String? = null,
        task_description: String? = null,
        task_dependencies: List<String>? = null,
        input_files: List<String>? = null,
        output_files: List<String>? = null,
        state: TaskState? = null
    ) : PlanTaskBase(
        task_type = task_type,
        task_description = task_description,
        task_dependencies = task_dependencies,
        input_files = input_files,
        output_files = output_files,
        state = state
    )

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
            model = planSettings.getTaskSettings(planTask?.task_type!! as TaskType<*>).model
                ?: planSettings.defaultModel,
            temperature = planSettings.temperature,
        )
    }

    override fun promptSegment(): String {
        return """
 Inquiry - Answer questions by reading in files and providing a summary that can be discussed with and approved by the user
   ** Specify the questions and the goal of the inquiry
   ** List input files to be examined when answering the questions
            """.trimMargin()
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
        val toInput = { it: String ->
            listOf<String>(
                userMessage,
                JsonUtil.toJson(plan),
                getPriorCode(planProcessingState),
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
            reviseResponse = { userMessages: List<Pair<String, ApiModel.Role>> ->
                val inStr = "Expand ${this.planTask?.task_description ?: ""}\nQuestions: ${
                    planTask?.inquiry_questions?.joinToString("\n")
                }\nGoal: ${planTask?.inquiry_goal}\n${JsonUtil.toJson(data = this)}"
                val messages = userMessages.map { ApiModel.ChatMessage(it.second, it.first.toContentList()) }
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
        planProcessingState.taskResult[taskId] = inquiryResult
    }

    companion object {
        private val log = LoggerFactory.getLogger(InquiryTask::class.java)
    }
}