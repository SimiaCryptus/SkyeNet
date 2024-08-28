package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.AgentPatterns
import com.simiacryptus.skyenet.Discussable
import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.core.actors.ParsedResponse
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.util.MarkdownUtil
import org.slf4j.LoggerFactory
import java.util.concurrent.ThreadPoolExecutor

class TaskPlanningTask(
    settings: Settings,
    task: PlanCoordinator.Task
) : AbstractTask(settings, task) {
    val taskBreakdownActor by lazy { settings.planningActor() }

    override fun promptSegment(): String {
        return """
            TaskPlanning - High-level planning and organization of tasks - identify smaller, actionable tasks based on the information available at task execution time.
              ** Specify the prior tasks and the goal of the task
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
        if (!agent.settings.taskPlanningEnabled) throw RuntimeException("Task planning is disabled")
        fun toInput(s: String) = listOf(
            userMessage,
            plan.text,
            getPriorCode(genState),
            getInputFileCode(),
            s
        ).filter { it.isNotBlank() }

        val subPlan = Discussable(
            task = task,
            userMessage = { "Expand ${description ?: ""}\n${JsonUtil.toJson(task)}" },
            heading = "",
            initialResponse = { it: String -> taskBreakdownActor.answer(toInput(it), api = agent.api) },
            outputFn = { design: ParsedResponse<PlanCoordinator.TaskBreakdownResult> ->
                AgentPatterns.displayMapInTabs(
                    mapOf(
                        "Text" to MarkdownUtil.renderMarkdown(design.text, ui = agent.ui),
                        "JSON" to MarkdownUtil.renderMarkdown(
                            "${TRIPLE_TILDE}json\n${JsonUtil.toJson(design.obj)/*.indent("  ")*/}\n$TRIPLE_TILDE",
                            ui = agent.ui
                        ),
                    )
                )
            },
            ui = agent.ui,
            reviseResponse = { userMessages: List<Pair<String, ApiModel.Role>> ->
                taskBreakdownActor.respond(
                    messages = (userMessages.map { ApiModel.ChatMessage(it.second, it.first.toContentList()) }
                        .toTypedArray<ApiModel.ChatMessage>()),
                    input = toInput("Expand ${description ?: ""}\n${JsonUtil.toJson(this)}"),
                    api = agent.api
                )
            },
        ).call()
        // Execute sub-tasks
        val subTasks = subPlan.obj.tasksByID ?: emptyMap()
        val pool: ThreadPoolExecutor = ApplicationServices.clientManager.getPool(agent.session, agent.user)
        val subGenState = PlanCoordinator.GenState(subTasks.toMutableMap())
        PlanCoordinator.executionOrder(subTasks).forEach { subTaskId ->
            val subTask = subTasks[subTaskId] ?: return@forEach
            val subTaskImpl = agent.settings.getImpl(subTask)
            pool.submit {
                try {
                    val subTaskTask = agent.ui.newTask(false)
                    taskTabs[subTaskId] = subTaskTask.placeholder
                    subTaskImpl.run(
                        agent = agent,
                        taskId = subTaskId,
                        userMessage = userMessage,
                        plan = subPlan,
                        genState = subGenState,
                        task = subTaskTask,
                        taskTabs = taskTabs
                    )
                } catch (e: Throwable) {
                    log.error("Error executing sub-task $subTaskId", e)
                }
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(TaskPlanningTask::class.java)
    }
}