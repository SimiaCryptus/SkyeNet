package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.core.actors.ParsedResponse
import com.simiacryptus.skyenet.webui.session.SessionTask
import org.slf4j.LoggerFactory

class ForeachTask(
    settings: Settings,
    planTask: PlanTask
) : AbstractTask(settings, planTask) {

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
        plan: PlanCoordinator.TaskBreakdownResult,
        genState: PlanCoordinator.GenState,
        task: SessionTask,
        taskTabs: TabbedDisplay
    ) {
        val items = planTask.foreachItems ?: throw RuntimeException("No items specified for ForeachTask")
        items.forEachIndexed { index, item ->
            val subTask = agent.ui.newTask(false)
            task.add(subTask.placeholder)
            
            // Create a new PlanTask for each item, copying the original task's properties
            val itemTask = planTask.copy(
                description = "${planTask.description} - Item $index: $item",
                foreachItems = null // Remove the foreach items to prevent infinite recursion
            )
            
            // Execute the task for this item
            val subTaskImpl = settings.getImpl(itemTask)
            subTaskImpl.run(agent, "$taskId-$index", userMessage, plan, genState, subTask, taskTabs)
        }
        task.complete("Completed ForeachTask for ${items.size} items")
    }

    companion object {
        private val log = LoggerFactory.getLogger(ForeachTask::class.java)
    }
}