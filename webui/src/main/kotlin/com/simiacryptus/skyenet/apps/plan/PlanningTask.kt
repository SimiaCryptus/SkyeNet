package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.models.ApiModel
import com.simiacryptus.jopenai.models.ApiModel.Role
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.skyenet.Discussable
import com.simiacryptus.skyenet.apps.plan.PlanUtil.diagram
import com.simiacryptus.skyenet.apps.plan.PlanUtil.executionOrder
import com.simiacryptus.skyenet.apps.plan.PlanUtil.filterPlan
import com.simiacryptus.skyenet.apps.plan.PlanUtil.render
import com.simiacryptus.skyenet.apps.plan.PlanningTask.PlanningTaskData
import com.simiacryptus.skyenet.core.actors.ParsedResponse
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.util.JsonUtil
import org.slf4j.LoggerFactory

class PlanningTask(
    planSettings: PlanSettings,
    planTask: PlanningTaskData?
) : AbstractTask<PlanningTaskData>(planSettings, planTask) {

    class PlanningTaskData(
        task_description: String? = null,
        task_dependencies: List<String>? = null,
        state: TaskState? = TaskState.Pending,
    ) : PlanTaskBase(
        task_type = TaskType.TaskPlanning.name,
        task_description = task_description,
        task_dependencies = task_dependencies,
        state = state
    )

    data class TaskBreakdownResult(
        @Description("A map where each task ID is associated with its corresponding PlanTask object. Crucial for defining task relationships and information flow.")
        val tasksByID: Map<String, PlanTaskBase>? = null,
    )

    override fun promptSegment(): String {
        return """
        |Task Planning:
        |  * Perform high-level planning and organization of tasks.
        |  * Decompose the overall goal into smaller, actionable tasks based on current information, ensuring proper information flow between tasks.
        |  * Specify prior tasks and the overall goal of the task, emphasizing dependencies to ensure each task is connected with its upstream and downstream tasks.
        |  * Dynamically break down tasks as new information becomes available.
        |  * Carefully consider task dependencies to ensure efficient information transfer and coordination between tasks.
        |  * Design the task structure to maximize parallel execution where possible, while respecting necessary dependencies.
        |  * **Note**: A planning task should refine the plan based on new information, optimizing task relationships and dependencies, and should not initiate execution.
        |  * Ensure that each task utilizes the outputs or side effects of its upstream tasks, and provides outputs or side effects for its downstream tasks.
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
        val userMessage = messages.joinToString("\n")
        val newTask = agent.ui.newTask(false).apply { add(placeholder) }
        fun toInput(s: String) = (messages + listOf(s)).filter { it.isNotBlank() }

        val subPlan = if (planSettings.allowBlocking && !planSettings.autoFix) {
            createSubPlanDiscussable(newTask, userMessage, ::toInput, api, agent.ui).call().obj
        } else {
            val design = planSettings.planningActor().answer(
                toInput("Expand ${planTask?.task_description ?: ""}"),
                api = api
            )
            render(
                withPrompt = TaskBreakdownWithPrompt(
                    plan = filterPlan { design.obj.tasksByID } ?: emptyMap(),
                    planText = design.text,
                    prompt = userMessage
                ),
                ui = agent.ui
            )
            design.obj
        }
        executeSubTasks(agent, userMessage, filterPlan { subPlan.tasksByID } ?: emptyMap(), task, api)
    }

    private fun createSubPlanDiscussable(
        task: SessionTask,
        userMessage: String,
        toInput: (String) -> List<String>,
        api: API,
        ui: ApplicationInterface
    ) = Discussable(
        task = task,
        userMessage = { "Expand ${planTask?.task_description ?: ""}" },
        heading = "",
        initialResponse = { it: String -> planSettings.planningActor().answer(toInput(it), api = api) },
        outputFn = { design: ParsedResponse<TaskBreakdownResult> ->
            render(
                withPrompt = TaskBreakdownWithPrompt(
                    plan = filterPlan { design.obj.tasksByID } ?: emptyMap(),
                    planText = design.text,
                    prompt = userMessage
                ),
                ui = ui
            )
        },
        ui = ui,
        reviseResponse = { usermessages: List<Pair<String, Role>> ->
            planSettings.planningActor().respond(
                messages = usermessages.map { ApiModel.ChatMessage(it.second, it.first.toContentList()) }
                    .toTypedArray<ApiModel.ChatMessage>(),
                input = toInput("Expand ${planTask?.task_description ?: ""}\n${JsonUtil.toJson(this)}"),
                api = api
            )
        },
    )

    private fun executeSubTasks(
        agent: PlanCoordinator,
        userMessage: String,
        subPlan: Map<String, PlanTaskBase>,
        parentTask: SessionTask,
        api: API
    ) {
        val subPlanTask = agent.ui.newTask(false)
        parentTask.add(subPlanTask.placeholder)
        val subTasks = subPlan ?: emptyMap()
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