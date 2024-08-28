package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.skyenet.apps.plan.PlanCoordinator.Task
import com.simiacryptus.skyenet.apps.plan.PlanCoordinator.TaskBreakdownResult
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.skyenet.core.actors.ParsedActor

data class Settings(
    val model: ChatModels,
    val parsingModel: ChatModels,
    val command: List<String>,
    val temperature: Double = 0.2,
    val budget: Double = 2.0,
    val taskPlanningEnabled: Boolean = false,
    val shellCommandTaskEnabled: Boolean = true,
    val autoFix: Boolean = false,
    val enableCommandAutoFix: Boolean = false,
    var commandAutoFixCommands: List<String> = listOf(),
    val env: Map<String, String> = mapOf(),
    val workingDir: String = ".",
    val language: String = if (PlanCoordinator.isWindows) "powershell" else "bash",
) {
    fun getImpl(task: Task): AbstractTask {
        return when (task.taskType) {
            TaskType.TaskPlanning -> TaskPlanningTask(this, task)
            TaskType.Documentation -> DocumentationTask(this, task)
            TaskType.NewFile -> NewFileTask(this, task)
            TaskType.EditFile -> EditFileTask(this, task)
            TaskType.RunShellCommand -> RunShellCommandTask(this, task)
            TaskType.CommandAutoFix -> CommandAutoFixTask(this, task)
            TaskType.Inquiry -> InquiryTask(this, task)
            else -> throw RuntimeException("Unknown task type: ${task.taskType}")
        }
    }

    fun planningActor() = ParsedActor(
        name = "TaskBreakdown",
        resultClass = TaskBreakdownResult::class.java,
        prompt = """
                    |Given a user request, identify and list smaller, actionable tasks that can be directly implemented in code.
                    |Detail files input and output as well as task execution dependencies.
                    |Creating directories and initializing source control are out of scope.
                    |
                    |Tasks can be of the following types: 
                    |
                    |
                    |${getAvailableTaskTypes().joinToString("\n") { "* ${it.promptSegment()}" }}
                    """.trimMargin(),
        model = this.model,
        parsingModel = this.parsingModel,
        temperature = this.temperature,
    )

    private fun getAvailableTaskTypes(): List<AbstractTask> = TaskType.values().filter {
        when (it) {
            TaskType.TaskPlanning -> this.taskPlanningEnabled
            TaskType.RunShellCommand -> this.shellCommandTaskEnabled
            else -> true
        }
    }.map { this.getImpl(Task(taskType = it)) }
}