package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.skyenet.apps.plan.ForeachTask.ForeachTaskData
import com.simiacryptus.skyenet.apps.plan.PlanUtil.diagram
import com.simiacryptus.skyenet.apps.plan.PlanUtil.executionOrder
import com.simiacryptus.skyenet.webui.session.SessionTask
import org.slf4j.LoggerFactory

class ForeachTask(
    planSettings: PlanSettings,
    planTask: ForeachTaskData?
) : AbstractTask<ForeachTaskData>(planSettings, planTask) {

    class ForeachTaskData(
        @Description("A list of items over which the ForEach task will iterate. (Only applicable for ForeachTask tasks) Can be used to process outputs from previous tasks.")
        val foreach_items: List<String>? = null,
        @Description("A map of sub-task IDs to PlanTask objects to be executed for each item. (Only applicable for ForeachTask tasks) Allows for complex task dependencies and information flow within iterations.")
        val foreach_subplan: Map<String, PlanTaskBase>? = null,
        task_type: String? = null,
        task_description: String? = null,
        task_dependencies: List<String>? = null,
        input_files: List<String>? = null,
        output_files: List<String>? = null,
        state: TaskState? = null,
    ) : PlanTaskBase(
        task_type = task_type,
        task_description = task_description,
        task_dependencies = task_dependencies,
        input_files = input_files,
        output_files = output_files,
        state = state
    )

    override fun promptSegment(): String {
        return """
ForeachTask - Execute a task for each item in a list
  ** Specify the list of items to iterate over
  ** Define the task to be executed for each item
        """.trimIndent()
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
        val items =
            planTask?.foreach_items ?: throw RuntimeException("No items specified for ForeachTask")
        val subTasks = planTask.foreach_subplan ?: throw RuntimeException("No subTasks specified for ForeachTask")
        val subPlanTask = agent.ui.newTask(false)
        task.add(subPlanTask.placeholder)
        
        items.forEachIndexed { index, item ->
            val itemSubTasks = subTasks.mapValues { (_, subTaskPlan) ->
                subTaskPlan.task_description = "${subTaskPlan.task_description} - Item $index: $item"
                subTaskPlan
            }
            val itemPlanProcessingState = PlanProcessingState(itemSubTasks)
            agent.executePlan(
                task = subPlanTask,
                diagramBuffer = subPlanTask.add(diagram(agent.ui, itemPlanProcessingState.subTasks)),
                subTasks = itemSubTasks,
                diagramTask = subPlanTask,
                planProcessingState = itemPlanProcessingState,
                taskIdProcessingQueue = executionOrder(itemSubTasks).toMutableList(),
                pool = agent.pool,
                userMessage = "$userMessage\nProcessing item $index: $item",
                plan = itemSubTasks,
                api = api
            )
        }
        subPlanTask.complete("Completed ForeachTask for ${items.size} items")
    }

    companion object {
        private val log = LoggerFactory.getLogger(ForeachTask::class.java)
    }
}