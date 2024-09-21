package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.jopenai.API
import com.simiacryptus.skyenet.apps.plan.AbstractTask.PlanTaskBaseInterface
import com.simiacryptus.skyenet.apps.plan.ForeachTask.ForeachTaskInterface
import com.simiacryptus.skyenet.apps.plan.PlanUtil.diagram
import com.simiacryptus.skyenet.apps.plan.PlanUtil.executionOrder
import com.simiacryptus.skyenet.apps.plan.PlanningTask.*
import com.simiacryptus.skyenet.webui.session.SessionTask
import org.slf4j.LoggerFactory

class ForeachTask<T: PlanTaskBaseInterface>(
    planSettings: PlanSettings,
    planTask: ForeachTaskInterface<T>?
) : AbstractTask<ForeachTaskInterface<T>>(planSettings, planTask) {
    interface ForeachTaskInterface<T : PlanTaskBaseInterface> : PlanTaskBaseInterface {
        val foreach_task: ForEachTask<T>?
    }

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
        plan: Map<String, PlanTaskBaseInterface>,
        planProcessingState: PlanProcessingState,
        task: SessionTask,
        api: API
    ) {
        val items =
            planTask?.foreach_task?.foreach_items ?: throw RuntimeException("No items specified for ForeachTask")
        val subTasks = planTask.foreach_task?.foreach_subplan ?: throw RuntimeException("No subTasks specified for ForeachTask")
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