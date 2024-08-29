package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.Discussable
import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.core.actors.ParsedResponse
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.util.MarkdownUtil
import org.slf4j.LoggerFactory
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference

class InquiryTask(
    settings: Settings,
    task: PlanCoordinator.Task
) : AbstractTask(settings, task) {
    val inquiryActor by lazy {
        SimpleActor(
            name = "Inquiry",
            prompt = """
                |Create code for a new file that fulfills the specified requirements and context.
                |Given a detailed user request, break it down into smaller, actionable tasks suitable for software development.
                |Compile comprehensive information and insights on the specified topic.
                |Provide a comprehensive overview, including key concepts, relevant technologies, best practices, and any potential challenges or considerations. 
                |Ensure the information is accurate, up-to-date, and well-organized to facilitate easy understanding.
                
                |When generating insights, consider the existing project context and focus on information that is directly relevant and applicable.
                |Focus on generating insights and information that support the task types available in the system (Requirements, NewFile, EditFile, ${
                    if (!settings.taskPlanningEnabled) "" else "TaskPlanning, "
                }${
                    if (!settings.shellCommandTaskEnabled) "" else "RunShellCommand, "
                }Documentation).
                |This will ensure that the inquiries are tailored to assist in the planning and execution of tasks within the system's framework.
                """.trimMargin(),
            model = settings.model,
            temperature = settings.temperature,
        )
    }

    override fun promptSegment(): String {
        return """
            |Inquiry - Answer questions by reading in files and providing a summary that can be discussed with and approved by the user
            |  ** Specify the questions and the goal of the inquiry
            |  ** List input files to be examined when answering the questions
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
        val toInput = { it: String ->
            listOf<String>(
                userMessage,
                plan.text,
                getPriorCode(genState),
                getInputFileCode(),
                it,
            ).filter { it.isNotBlank() }
        }
        val inquiryResult = Discussable(
            task = task,
            userMessage = { "Expand ${this.description ?: ""}\n${JsonUtil.toJson(data = this)}" },
            heading = "",
            initialResponse = { it: String -> inquiryActor.answer(toInput(it), api = agent.api) },
            outputFn = { design: String ->
                MarkdownUtil.renderMarkdown(design, ui = agent.ui)
            },
            ui = agent.ui,
            reviseResponse = { userMessages: List<Pair<String, ApiModel.Role>> ->
                inquiryActor.respond(
                    messages = (userMessages.map { ApiModel.ChatMessage(it.second, it.first.toContentList()) }
                        .toTypedArray<ApiModel.ChatMessage>()),
                    input = toInput("Expand ${this.description ?: ""}\n${JsonUtil.toJson(data = this)}"),
                    api = agent.api
                )
            },
            atomicRef = AtomicReference(),
            semaphore = Semaphore(0),
        ).call()
        genState.taskResult[taskId] = inquiryResult
    }

    companion object {
        private val log = LoggerFactory.getLogger(InquiryTask::class.java)
    }
}