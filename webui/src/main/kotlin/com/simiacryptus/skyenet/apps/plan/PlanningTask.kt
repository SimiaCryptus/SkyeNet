package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.AgentPatterns
import com.simiacryptus.skyenet.Discussable
import com.simiacryptus.skyenet.Retryable
import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.apps.plan.PlanUtil.diagram
import com.simiacryptus.skyenet.apps.plan.PlanUtil.executionOrder
import com.simiacryptus.skyenet.apps.plan.PlanUtil.filterPlan
import com.simiacryptus.skyenet.apps.plan.TaskType.Companion.getAvailableTaskTypes
import com.simiacryptus.skyenet.core.actors.ParsedActor
import com.simiacryptus.skyenet.core.actors.ParsedResponse
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.util.MarkdownUtil
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

    private val taskBreakdownActor by lazy { planningActor(planSettings) }

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
        taskTabs: TabbedDisplay
    ) {
        if (!agent.planSettings.taskPlanningEnabled) throw RuntimeException("Task planning is disabled")
        @Suppress("NAME_SHADOWING") val task = agent.ui.newTask(false).apply { task.add(placeholder) }
        fun toInput(s: String) = listOf(
            userMessage,
            JsonUtil.toJson(plan),
            getPriorCode(planProcessingState),
            getInputFileCode(),
            s
        ).filter { it.isNotBlank() }
        if (!planSettings.autoFix) {
            val subPlan = Discussable(
                task = task,
                userMessage = { "Expand ${planTask.description ?: ""}" },
                heading = "",
                initialResponse = { it: String -> taskBreakdownActor.answer(toInput(it), api = agent.api) },
                outputFn = { design: ParsedResponse<TaskBreakdownResult> ->
                    val ui = PlanProcessingState(
                        (filterPlan(design.obj).tasksByID ?: emptyMap()).toMutableMap()
                    )
                    AgentPatterns.displayMapInTabs(
                        mapOf(
                            "Text" to MarkdownUtil.renderMarkdown(design.text, ui = agent.ui),
                            "JSON" to MarkdownUtil.renderMarkdown(
                                "${TRIPLE_TILDE}json\n${JsonUtil.toJson(filterPlan(design.obj))/*.indent("  ")*/}\n$TRIPLE_TILDE",
                                ui = agent.ui
                            ),
                            "Diagram" to diagram(
                                agent.ui, ui.subTasks
                            )
                        )
                    )
                },
                ui = agent.ui,
                reviseResponse = { userMessages: List<Pair<String, ApiModel.Role>> ->
                    taskBreakdownActor.respond(
                        messages = (userMessages.map { ApiModel.ChatMessage(it.second, it.first.toContentList()) }
                            .toTypedArray<ApiModel.ChatMessage>()),
                        input = toInput("Expand ${planTask.description ?: ""}\n${JsonUtil.toJson(this)}"),
                        api = agent.api
                    )
                },
            ).call()
            // Execute sub-tasks
            executeSubTasks(agent, userMessage, filterPlan(subPlan.obj), task)
        } else {
            val subPlan = taskBreakdownActor.answer(toInput("Expand ${planTask.description ?: ""}"), api = agent.api)
            // Execute sub-tasks
            Retryable(agent.ui,task) {
                val task = agent.ui.newTask(false)
                executeSubTasks(agent, userMessage, filterPlan(subPlan.obj), task)
                task.placeholder
            }
        }
    }
    private fun executeSubTasks(
        agent: PlanCoordinator,
        userMessage: String,
        subPlan: TaskBreakdownInterface,
        parentTask: SessionTask
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
        )
        subPlanTask.complete()
    }

    companion object {
        private val log = LoggerFactory.getLogger(PlanningTask::class.java)
        fun planningActor(planSettings: PlanSettings) = ParsedActor(
            name = "TaskBreakdown",
            resultClass = TaskBreakdownResult::class.java,
            prompt = """
                        |Given a user request, identify and list smaller, actionable tasks that can be directly implemented in code.
                        |Detail files input and output as well as task execution dependencies.
                        |Creating directories and initializing source control are out of scope.
                        |
                        |Tasks can be of the following types: 
                        |
                        |${getAvailableTaskTypes(planSettings).joinToString("\n") { "* ${it.promptSegment()}" }}
                        |
                        |${if (planSettings.taskPlanningEnabled) "Do not start your plan with a plan to plan!\n" else ""}
                        """.trimMargin(),
            model = planSettings.model,
            parsingModel = planSettings.parsingModel,
            temperature = planSettings.temperature,
        )

    }
}