package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.jopenai.models.OpenAITextModel
import com.simiacryptus.skyenet.apps.plan.PlanUtil.isWindows

data class TaskSettings(
    var enabled: Boolean = false
)


data class PlanSettings(
    var model: OpenAITextModel,
    val parsingModel: OpenAITextModel,
    val command: List<String> = listOf(if (isWindows) "powershell" else "bash"),
    var temperature: Double = 0.2,
    val budget: Double = 2.0,
    val taskSettings: MutableMap<TaskType, TaskSettings> = mutableMapOf(),
    var autoFix: Boolean = false,
    var allowBlocking: Boolean = true,
    var enableCommandAutoFix: Boolean = false,
    var commandAutoFixCommands: List<String>? = listOf(),
    val env: Map<String, String>? = mapOf(),
    val workingDir: String? = ".",
    val language: String? = if (isWindows) "powershell" else "bash",
) {
    init {
        TaskType.values().forEach { taskType ->
            taskSettings[taskType] = TaskSettings(
                when (taskType) {
                    TaskType.FileModification, TaskType.Inquiry -> true
                    else -> false
                }
            )
        }
    }

    fun getTaskSettings(taskType: TaskType): TaskSettings =
        taskSettings[taskType] ?: TaskSettings()

    fun setTaskSettings(taskType: TaskType, settings: TaskSettings) {
        taskSettings[taskType] = settings
    }

}