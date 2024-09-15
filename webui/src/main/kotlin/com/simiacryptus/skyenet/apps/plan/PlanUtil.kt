package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.AgentPatterns
import com.simiacryptus.skyenet.apps.plan.AbstractTask.TaskState
import com.simiacryptus.skyenet.apps.plan.PlanningTask.PlanTask
import com.simiacryptus.skyenet.apps.plan.PlanningTask.TaskBreakdownInterface
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.util.MarkdownUtil
import java.util.*

object PlanUtil {

    fun diagram(
        ui: ApplicationInterface,
        taskMap: Map<String, PlanTask>
    ) = MarkdownUtil.renderMarkdown(
        """
            |## Sub-Plan Task Dependency Graph
            |${TRIPLE_TILDE}mermaid
            |${buildMermaidGraph(taskMap)}
            |$TRIPLE_TILDE
            """.trimMargin(),
        ui = ui
    )

    data class TaskBreakdownWithPrompt(
        val prompt: String,
        val plan: PlanningTask.TaskBreakdownResult,
        val planText: String
    )

    fun render(
        withPrompt: TaskBreakdownWithPrompt,
        ui: ApplicationInterface
    ) = AgentPatterns.displayMapInTabs(
        mapOf(
            "Text" to MarkdownUtil.renderMarkdown(withPrompt.planText, ui = ui),
            "JSON" to MarkdownUtil.renderMarkdown(
                "${TRIPLE_TILDE}json\n${JsonUtil.toJson(withPrompt)}\n$TRIPLE_TILDE",
                ui = ui
            ),
            "Diagram" to MarkdownUtil.renderMarkdown(
                "```mermaid\n" + buildMermaidGraph(
                    (filterPlan {
                        withPrompt.plan
                    }.tasksByID ?: emptyMap()).toMutableMap()
                ) + "\n```\n", ui = ui
            )
        )
    )

    fun executionOrder(tasks: Map<String, PlanTask>): List<String> {
        val taskIds: MutableList<String> = mutableListOf()
        val taskMap = tasks.mapValues { it.value.copy(task_dependencies = it.value.task_dependencies?.filter { entry ->
            entry in tasks.keys
        }) }.toMutableMap()
        while (taskMap.isNotEmpty()) {
            val nextTasks =
                taskMap.filter { (_, task) -> task.task_dependencies?.all { taskIds.contains(it) } ?: true }
            if (nextTasks.isEmpty()) {
                throw RuntimeException("Circular dependency detected in task breakdown")
            }
            taskIds.addAll(nextTasks.keys)
            nextTasks.keys.forEach { taskMap.remove(it) }
        }
        return taskIds
    }

    val isWindows = System.getProperty("os.name").lowercase(Locale.getDefault()).contains("windows")
    private fun sanitizeForMermaid(input: String) = input
        .replace(" ", "_")
        .replace("\"", "\\\"")
        .replace("[", "\\[")
        .replace("]", "\\]")
        .replace("(", "\\(")
        .replace(")", "\\)")
        .let { "`$it`" }

    private fun escapeMermaidCharacters(input: String) = input
        .replace("\"", "\\\"")
        .let { '"' + it + '"' }

    fun buildMermaidGraph(subTasks: Map<String, PlanTask>): String {
        val graphBuilder = StringBuilder("graph TD;\n")
        subTasks.forEach { (taskId, task) ->
            val sanitizedTaskId = sanitizeForMermaid(taskId)
            val taskType = task.task_type?.name ?: "Unknown"
            val escapedDescription = escapeMermaidCharacters(task.task_description ?: "")
            val style = when (task.state) {
                TaskState.Completed -> ":::completed"
                TaskState.InProgress -> ":::inProgress"
                else -> ":::$taskType"
            }
            graphBuilder.append("    ${sanitizedTaskId}[$escapedDescription]$style;\n")
            task.task_dependencies?.forEach { dependency ->
                val sanitizedDependency = sanitizeForMermaid(dependency)
                graphBuilder.append("    $sanitizedDependency --> ${sanitizedTaskId};\n")
            }
        }
        graphBuilder.append("    classDef default fill:#f9f9f9,stroke:#333,stroke-width:2px;\n")
        graphBuilder.append("    classDef NewFile fill:lightblue,stroke:#333,stroke-width:2px;\n")
        graphBuilder.append("    classDef EditFile fill:lightgreen,stroke:#333,stroke-width:2px;\n")
        graphBuilder.append("    classDef Documentation fill:lightyellow,stroke:#333,stroke-width:2px;\n")
        graphBuilder.append("    classDef Inquiry fill:orange,stroke:#333,stroke-width:2px;\n")
        graphBuilder.append("    classDef TaskPlanning fill:lightgrey,stroke:#333,stroke-width:2px;\n")
        graphBuilder.append("    classDef completed fill:#90EE90,stroke:#333,stroke-width:2px;\n")
        graphBuilder.append("    classDef inProgress fill:#FFA500,stroke:#333,stroke-width:2px;\n")
        return graphBuilder.toString()
    }

    fun filterPlan(retries: Int = 3, fn: () -> TaskBreakdownInterface): TaskBreakdownInterface {
        val obj = fn()
        var tasksByID = obj.tasksByID?.filter { (k, v) ->
            when {
                v.task_type == TaskType.TaskPlanning && v.task_dependencies.isNullOrEmpty() ->
                    if (retries <= 0) {
                        log.warn("TaskPlanning task $k has no dependencies: " + JsonUtil.toJson(obj))
                        true
                    } else {
                        log.info("TaskPlanning task $k has no dependencies")
                        return filterPlan(retries - 1, fn)
                    }
                else -> true
            }
        } ?: emptyMap()
        tasksByID = tasksByID.map {
            it.key to it.value.copy(
                task_dependencies = it.value.task_dependencies?.filter { it in tasksByID.keys },
                state = TaskState.Pending
            )
        }.toMap()
        try {
            executionOrder(tasksByID)
        } catch (e: RuntimeException) {
            if (retries <= 0) {
                log.warn("Error filtering plan: " + JsonUtil.toJson(obj), e)
                throw e
            } else {
                log.info("Circular dependency detected in task breakdown")
                return filterPlan(retries - 1, fn)
            }
        }
        return if (tasksByID.size == obj.tasksByID?.size) {
            obj
        } else filterPlan {
            PlanningTask.TaskBreakdownResult(tasksByID)
        }
    }

    fun getAllDependencies(
        subPlanTask: PlanTask,
        subTasks: Map<String, PlanTask>,
        visited: MutableSet<String>
    ): List<String> {
        val dependencies = subPlanTask.task_dependencies?.toMutableList() ?: mutableListOf()
        subPlanTask.task_dependencies?.forEach { dep ->
            if (dep in visited) return@forEach
            val subTask = subTasks[dep]
            if (subTask != null) {
                visited.add(dep)
                dependencies.addAll(getAllDependencies(subTask, subTasks, visited))
            }
        }
        return dependencies
    }

    val log = org.slf4j.LoggerFactory.getLogger(PlanUtil::class.java)

}