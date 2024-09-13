package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.Discussable
import com.simiacryptus.skyenet.apps.plan.PlanUtil.diagram
import com.simiacryptus.skyenet.apps.plan.PlanUtil.executionOrder
import com.simiacryptus.skyenet.apps.plan.PlanUtil.filterPlan
import com.simiacryptus.skyenet.apps.plan.PlanUtil.render
import com.simiacryptus.skyenet.core.actors.ParsedResponse
import com.simiacryptus.skyenet.webui.session.SessionTask
import org.slf4j.LoggerFactory

class PlanningTask(
    planSettings: PlanSettings,
    planTask: PlanTask
) : AbstractTask(planSettings, planTask) {


    interface TaskBreakdownInterface {
        val tasksByID: Map<String, PlanTask>?
        val finalTaskID: String?
    }

    data class TaskBreakdownResult(
        override val tasksByID: Map<String, PlanTask>? = null,
        override val finalTaskID: String? = null,
    ) : TaskBreakdownInterface

    data class PlanTask(
        val description: String? = null,
        val taskType: TaskType? = null,
        var task_dependencies: List<String>? = null,
        val input_files: List<String>? = null,
        val output_files: List<String>? = null,
        var state: TaskState? = null,
        @Description("Command and arguments (in list form) for the task")
        val command: List<String>? = null,
        @Description("Working directory for the command execution")
        val workingDir: String? = null,
        @Description("List of items to iterate over")
        val foreachItems: List<String>? = null,
        @Description("When applicable, sub-tasks to execute")
        val subTasksByID: Map<String, PlanTask>? = null,
    )

    private val taskBreakdownActor by lazy { planSettings.planningActor() }

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
        plan: TaskBreakdownInterface,
        planProcessingState: PlanProcessingState,
        task: SessionTask,
        api: API
    ) {
        if (!agent.planSettings.getTaskSettings(TaskType.TaskPlanning).enabled) throw RuntimeException("Task planning is disabled")
        @Suppress("NAME_SHADOWING") val task = agent.ui.newTask(false).apply { task.add(placeholder) }
        fun toInput(s: String) = listOf(
            userMessage,
            JsonUtil.toJson(plan),
            getPriorCode(planProcessingState),
            getInputFileCode(),
            s
        ).filter { it.isNotBlank() }
        val subPlan = if (planSettings.allowBlocking && !planSettings.autoFix) {
            createSubPlanDiscussable(agent, task, userMessage, ::toInput, api).call().obj
        } else {
            val design = taskBreakdownActor.answer(
                toInput("Expand ${planTask.description ?: ""}"),
                api = api
            )
            render(
                withPrompt = PlanUtil.TaskBreakdownWithPrompt(
                    plan = filterPlan { design.obj } as TaskBreakdownResult,
                    planText = design.text,
                    prompt = userMessage
                ),
                ui = agent.ui
            )
            design.obj
        }
        executeSubTasks(agent, userMessage, filterPlan { subPlan }, task, api)
    }

    private fun createSubPlanDiscussable(
        agent: PlanCoordinator,
        task: SessionTask,
        userMessage: String,
        toInput: (String) -> List<String>,
        api: API
    ): Discussable<ParsedResponse<TaskBreakdownResult>> {
        return Discussable(
            task = task,
            userMessage = { "Expand ${planTask.description ?: ""}" },
            heading = "",
            initialResponse = { it: String -> taskBreakdownActor.answer(toInput(it), api = api) },
            outputFn = { design: ParsedResponse<TaskBreakdownResult> ->
                render(
                    withPrompt = PlanUtil.TaskBreakdownWithPrompt(
                        plan = filterPlan { design.obj } as TaskBreakdownResult,
                        planText = design.text,
                        prompt = userMessage
                    ),
                    ui = agent.ui
                )
            },
            ui = agent.ui,
            reviseResponse = { userMessages: List<Pair<String, ApiModel.Role>> ->
                taskBreakdownActor.respond(
                    messages = userMessages.map { ApiModel.ChatMessage(it.second, it.first.toContentList()) }
                        .toTypedArray<ApiModel.ChatMessage>(),
                    input = toInput("Expand ${planTask.description ?: ""}\n${JsonUtil.toJson(this)}"),
                    api = api
                )
            },
        )
    }
    private fun executeSubTasks(
        agent: PlanCoordinator,
        userMessage: String,
        subPlan: TaskBreakdownInterface,
        parentTask: SessionTask,
        api: API
    ) {
        val subPlanTask = agent.ui.newTask(false)
        parentTask.add(subPlanTask.placeholder)
        val subTasks = subPlan.tasksByID ?: emptyMap()
        val planProcessingState = PlanProcessingState(subTasks.toMutableMap())
        agent.executePlan(
            task = subPlanTask,
            diagramBuffer = subPlanTask.add(diagram(agent.ui, planProcessingState.subTasks)),
            subTasks = subTasks,
            diagramTask = subPlanTask,
            planProcessingState = planProcessingState,
            taskIdProcessingQueue = executionOrder(subTasks).toMutableList(),
            pool = agent.pool,
            userMessage = userMessage,
            plan = subPlan,
            api = api,
        )
        subPlanTask.complete()
    }

    companion object {
        private val log = LoggerFactory.getLogger(PlanningTask::class.java)
    }
}