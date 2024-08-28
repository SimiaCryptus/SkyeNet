package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.AgentPatterns
import com.simiacryptus.skyenet.Discussable
import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.apps.plan.PlanCoordinator.Companion.buildMermaidGraph
import com.simiacryptus.skyenet.core.actors.ParsedResponse
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.util.MarkdownUtil
import org.slf4j.LoggerFactory

class PlanningTask(
    settings: Settings,
    task: PlanCoordinator.Task
) : AbstractTask(settings, task) {
    val taskBreakdownActor by lazy { settings.planningActor() }

    override fun promptSegment(): String {
        return """
        |TaskPlanning - High-level planning and organization of tasks - identify smaller, actionable tasks based on the information available at task execution time.
        |  ** Specify the prior tasks and the goal of the task
        |  ** Used to dynamically break down tasks as needed given new information
        |  ** Important: A planning task should not be used to begin a plan, as no new knowledge will be present
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
        if (!agent.settings.taskPlanningEnabled) throw RuntimeException("Task planning is disabled")
        @Suppress("NAME_SHADOWING") val task = agent.ui.newTask(false).apply { task.add(placeholder) }
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
        executeSubTasks(agent, userMessage, subPlan, task)
    }
    private fun executeSubTasks(
        agent: PlanCoordinator,
        userMessage: String,
        subPlan: ParsedResponse<PlanCoordinator.TaskBreakdownResult>,
        parentTask: SessionTask
    ) {
        val subPlanTask = agent.ui.newTask(false)
        parentTask.add(subPlanTask.placeholder)
        val subTasks = subPlan.obj.tasksByID ?: emptyMap()
        val genState = PlanCoordinator.GenState(subTasks.toMutableMap())
        agent.executePlan(
            task = subPlanTask,
            diagramBuffer = subPlanTask.add(diagram(genState, agent.ui)),
            subTasks = subTasks,
            diagramTask = subPlanTask,
            genState = genState,
            taskIdProcessingQueue = PlanCoordinator.executionOrder(subTasks).toMutableList(),
            pool = agent.pool,
            userMessage = userMessage,
            plan = subPlan,
        )
        subPlanTask.complete()
    }

    private fun diagram(
        genState: PlanCoordinator.GenState,
        ui: ApplicationInterface
    ) = MarkdownUtil.renderMarkdown(
        """
        |## Sub-Plan Task Dependency Graph
        |${TRIPLE_TILDE}mermaid
        |${buildMermaidGraph(genState.subTasks)}
        |$TRIPLE_TILDE
        """.trimMargin(),
        ui = ui
    )

    companion object {
        private val log = LoggerFactory.getLogger(PlanningTask::class.java)
    }
}