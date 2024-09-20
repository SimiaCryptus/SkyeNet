package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.models.ApiModel
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.util.JsonUtil
import com.simiacryptus.skyenet.Discussable
import com.simiacryptus.skyenet.apps.plan.AbstractTask.PlanTaskBaseInterface
import com.simiacryptus.skyenet.apps.plan.ForeachTask.ForeachTaskInterface
import com.simiacryptus.skyenet.apps.plan.PlanUtil.diagram
import com.simiacryptus.skyenet.apps.plan.PlanUtil.executionOrder
import com.simiacryptus.skyenet.apps.plan.PlanUtil.filterPlan
import com.simiacryptus.skyenet.apps.plan.PlanUtil.render
import com.simiacryptus.skyenet.apps.plan.RunShellCommandTask.ExecutionTaskInterface
import com.simiacryptus.skyenet.core.actors.ParsedResponse
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask
import org.slf4j.LoggerFactory

class PlanningTask(
    planSettings: PlanSettings,
    planTask: PlanTaskBaseInterface?
) : AbstractTask<PlanTaskBaseInterface>(planSettings, planTask) {

    interface TaskBreakdownInterface {
        val tasksByID: Map<String, PlanTask>?
    }

    data class TaskBreakdownResult(
        @Description("A map where each task ID is associated with its corresponding PlanTask object. Crucial for defining task relationships and information flow.")
        override val tasksByID: Map<String, PlanTask>? = null,
    ) : TaskBreakdownInterface

    data class PlanTask(
        @Description("A detailed description of the specific task to be performed, including its role in the overall plan and its dependencies on other tasks.")
        override val task_description: String? = null,
        @Description("An enumeration indicating the type of task to be executed.")
        override val task_type: TaskType<*>? = null,
        @Description("A list of IDs of tasks that must be completed before this task can be executed. This defines upstream dependencies ensuring proper task order and information flow.")
        override var task_dependencies: List<String>? = null,
        @Description("A list of file paths specifying the input files required by this task. These may be outputs from dependent tasks, facilitating data transfer between tasks.")
        override val input_files: List<String>? = null,
        @Description("A list of file paths specifying the output files generated by this task. These may serve as inputs for subsequent tasks, enabling information sharing.")
        override val output_files: List<String>? = null,
        @Description("The current execution state of the task. Important for coordinating task execution and managing dependencies.")
        override var state: TaskState? = TaskState.Pending,
        @Description("Only applicable in Foreach tasks - details specific to the Foreach task.")
        override val foreach_task: ForEachTask? = null,
        @Description("Only applicable in CommandAutoFix tasks - details specific to the CommandAutoFix task.")
        override val execution_task: ExecutionTask? = null,
    ) : PlanTaskBaseInterface, ExecutionTaskInterface, ForeachTaskInterface

    data class ForEachTask(
        @Description("A list of items over which the ForEach task will iterate. (Only applicable for ForeachTask tasks) Can be used to process outputs from previous tasks.")
        val foreach_items: List<String>? = null,
        @Description("A map of sub-task IDs to PlanTask objects to be executed for each item. (Only applicable for ForeachTask tasks) Allows for complex task dependencies and information flow within iterations.")
        val foreach_subplan: Map<String, PlanTask>? = null,
    )

    data class ExecutionTask(
        @Description("The command line for the task (Only applicable in CommandAutoFix tasks).")
        val command: List<String>? = null,
        @Description("The working directory relative to the root directory (e.g., \".\" or \"./subdir\") (Only applicable for CommandAutoFix and RunShellCommand tasks)")
        val workingDir: String? = null,
    )

    private val taskBreakdownActor by lazy { planSettings.planningActor() }

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
        userMessage: String,
        plan: TaskBreakdownInterface,
        planProcessingState: PlanProcessingState,
        task: SessionTask,
        api: API
    ) {
        @Suppress("NAME_SHADOWING") val task = agent.ui.newTask(false).apply { task.add(placeholder) }
        fun toInput(s: String) = listOf(
            userMessage,
            JsonUtil.toJson(plan),
            getPriorCode(planProcessingState),
            getInputFileCode(),
            s
        ).filter { it.isNotBlank() }
        val subPlan = if (planSettings.allowBlocking && !planSettings.autoFix) {
            createSubPlanDiscussable(task, userMessage, ::toInput, api, agent.ui).call().obj
        } else {
            val design = taskBreakdownActor.answer(
                toInput("Expand ${planTask?.task_description ?: ""}"),
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
        task: SessionTask,
        userMessage: String,
        toInput: (String) -> List<String>,
        api: API,
        ui: ApplicationInterface
    ): Discussable<ParsedResponse<TaskBreakdownResult>> {
        return Discussable(
            task = task,
            userMessage = { "Expand ${planTask?.task_description ?: ""}" },
            heading = "",
            initialResponse = { it: String -> taskBreakdownActor.answer(toInput(it), api = api) },
            outputFn = { design: ParsedResponse<TaskBreakdownResult> ->
                render(
                    withPrompt = PlanUtil.TaskBreakdownWithPrompt(
                        plan = filterPlan { design.obj } as TaskBreakdownResult,
                        planText = design.text,
                        prompt = userMessage
                    ),
                    ui = ui
                )
            },
            ui = ui,
            reviseResponse = { userMessages: List<Pair<String, ApiModel.Role>> ->
                taskBreakdownActor.respond(
                    messages = userMessages.map { ApiModel.ChatMessage(it.second, it.first.toContentList()) }
                        .toTypedArray<ApiModel.ChatMessage>(),
                    input = toInput("Expand ${planTask?.task_description ?: ""}\n${JsonUtil.toJson(this)}"),
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