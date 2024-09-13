package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.jopenai.describe.AbbrevWhitelistYamlDescriber
import com.simiacryptus.jopenai.models.OpenAITextModel
import com.simiacryptus.skyenet.apps.plan.PlanUtil.isWindows
import com.simiacryptus.skyenet.apps.plan.PlanningTask.TaskBreakdownResult
import com.simiacryptus.skyenet.apps.plan.TaskType.Companion.getAvailableTaskTypes
import com.simiacryptus.skyenet.core.actors.ParsedActor

data class TaskSettings(
    var enabled: Boolean = false,
    var model: OpenAITextModel? = null
)


open class PlanSettings(
    val parsingModel: OpenAITextModel,
    val command: List<String> = listOf(if (isWindows) "powershell" else "bash"),
    var temperature: Double = 0.2,
    val budget: Double = 2.0,
    val taskSettings: MutableMap<TaskType, TaskSettings> = TaskType.values().associateWith { taskType ->
        TaskSettings(
            when (taskType) {
                TaskType.FileModification, TaskType.Inquiry -> true
                else -> false
            }
        )
    }.toMutableMap(),
    var autoFix: Boolean = false,
    var allowBlocking: Boolean = true,
    var commandAutoFixCommands: List<String>? = listOf(),
    val env: Map<String, String>? = mapOf(),
    val workingDir: String? = ".",
    val language: String? = if (isWindows) "powershell" else "bash",
) {

    fun getTaskSettings(taskType: TaskType): TaskSettings =
        taskSettings[taskType] ?: TaskSettings()

    fun setTaskSettings(taskType: TaskType, settings: TaskSettings) {
        taskSettings[taskType] = settings
    }

    fun copy(
        parsingModel: OpenAITextModel = this.parsingModel,
        command: List<String> = this.command,
        temperature: Double = this.temperature,
        budget: Double = this.budget,
        taskSettings: MutableMap<TaskType, TaskSettings> = this.taskSettings,
        autoFix: Boolean = this.autoFix,
        allowBlocking: Boolean = this.allowBlocking,
        commandAutoFixCommands: List<String>? = this.commandAutoFixCommands,
        env: Map<String, String>? = this.env,
        workingDir: String? = this.workingDir,
        language: String? = this.language,
    ) = PlanSettings(
        parsingModel = parsingModel,
        command = command,
        temperature = temperature,
        budget = budget,
        taskSettings = taskSettings,
        autoFix = autoFix,
        allowBlocking = allowBlocking,
        commandAutoFixCommands = commandAutoFixCommands,
        env = env,
        workingDir = workingDir,
        language = language,
    )

    open fun planningActor() = ParsedActor(
        name = "TaskBreakdown",
        resultClass = TaskBreakdownResult::class.java,
        prompt = """
     Given a user request, identify and list smaller, actionable tasks that can be directly implemented in code.
     Detail files input and output as well as task execution dependencies.
     Creating directories and initializing source control are out of scope.
                |
                |Tasks can be of the following types: 
                |
                |${getAvailableTaskTypes(this).joinToString("\n") { "* ${it.promptSegment()}" }}
                |
                |${if (this.getTaskSettings(TaskType.TaskPlanning).enabled) "Do not start your plan with a plan to plan!\n" else ""}
                """.trimMargin(),
        model = this.getTaskSettings(TaskType.TaskPlanning).model ?: this.parsingModel,
        parsingModel = this.parsingModel,
        temperature = this.temperature,
        describer = describer(),
    )

    open fun describer() = object : AbbrevWhitelistYamlDescriber(
        "com.simiacryptus", "com.github.simiacryptus"
    ) {
        override val includeMethods: Boolean get() = false

        override fun getEnumValues(clazz: Class<*>): List<String> {
            return if (clazz == TaskType::class.java) {
                taskSettings.filter { it.value.enabled }.map { it.key.toString() }
            } else {
                super.getEnumValues(clazz)
            }
        }

    }

}