package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.jopenai.describe.AbbrevWhitelistYamlDescriber
import com.simiacryptus.jopenai.models.ChatModel
import com.simiacryptus.skyenet.apps.plan.CommandAutoFixTask.CommandAutoFixTaskConfigData
import com.simiacryptus.skyenet.apps.plan.PlanUtil.isWindows
import com.simiacryptus.skyenet.apps.plan.PlanningTask.PlanningTaskConfigData
import com.simiacryptus.skyenet.apps.plan.PlanningTask.TaskBreakdownResult
import com.simiacryptus.skyenet.apps.plan.TaskType.Companion.getAvailableTaskTypes
import com.simiacryptus.skyenet.apps.plan.TaskType.Companion.getImpl
import com.simiacryptus.skyenet.apps.plan.file.FileModificationTask.FileModificationTaskConfigData
import com.simiacryptus.skyenet.core.actors.ParsedActor


data class TaskSettings(
  var enabled: Boolean = false,
  var model: ChatModel? = null
)

open class PlanSettings(
  var defaultModel: ChatModel,
  var parsingModel: ChatModel,
  val command: List<String> = listOf(if (isWindows) "powershell" else "bash"),
  var temperature: Double = 0.2,
  val budget: Double = 2.0,
  val taskSettings: MutableMap<String, TaskSettings> = TaskType.values().associateWith { taskType ->
    TaskSettings(
      when (taskType) {
        TaskType.FileModification, TaskType.Inquiry -> true
        else -> false
      }
    )
  }.mapKeys { it.key.name }.toMutableMap(),
  var autoFix: Boolean = false,
  var allowBlocking: Boolean = true,
  var commandAutoFixCommands: List<String>? = listOf(),
  val env: Map<String, String>? = mapOf(),
  val workingDir: String? = ".",
  val language: String? = if (isWindows) "powershell" else "bash",
  var githubToken: String? = null,
  var googleApiKey: String? = null,
  var googleSearchEngineId: String? = null,
) {

  fun getTaskSettings(taskType: TaskType<*>): TaskSettings =
    taskSettings[taskType.name] ?: TaskSettings()

  fun setTaskSettings(taskType: TaskType<*>, settings: TaskSettings) {
    taskSettings[taskType.name] = settings
  }

  fun copy(
    model: ChatModel = this.defaultModel,
    parsingModel: ChatModel = this.parsingModel,
    command: List<String> = this.command,
    temperature: Double = this.temperature,
    budget: Double = this.budget,
    taskSettings: MutableMap<String, TaskSettings> = this.taskSettings,
    autoFix: Boolean = this.autoFix,
    allowBlocking: Boolean = this.allowBlocking,
    commandAutoFixCommands: List<String>? = this.commandAutoFixCommands,
    env: Map<String, String>? = this.env,
    workingDir: String? = this.workingDir,
    language: String? = this.language,
  ) = PlanSettings(
    defaultModel = model,
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
    githubToken = this.githubToken,
    googleApiKey = this.googleApiKey,
    googleSearchEngineId = this.googleSearchEngineId,
  )

  fun planningActor(): ParsedActor<TaskBreakdownResult> {
    val planTaskSettings = this.getTaskSettings(TaskType.TaskPlanning)
    val prompt = """
                    |Given a user request, identify and list smaller, actionable tasks that can be directly implemented in code.
                    |
                    |For each task:
                    |* Detail files input and output file
                    |* Describe task execution dependencies and order
                    |* Provide a brief description of the task
                    |* Specify important interface and integration details (each task will run independently off of a copy of this plan)
                    |
                    |Tasks can be of the following types: 
                    |${
      getAvailableTaskTypes(this).joinToString("\n") { taskType ->
        "* ${getImpl(this, taskType).promptSegment()}"
      }
    }
                    |
                    |Creating directories and initializing source control are out of scope.
                    |${if (planTaskSettings.enabled) "Do not start your plan with a plan to plan!\n" else ""}
                    """.trimMargin()
    val describer = describer()
    val parserPrompt = """
Task Subtype Schema:

${
      getAvailableTaskTypes(this).joinToString("\n\n") { taskType ->
        """
${taskType.name}:
  ${describer.describe(taskType.taskDataClass).replace("\n", "\n  ")}
""".trim()
      }
    }
                """.trimIndent()
    return ParsedActor(
      name = "TaskBreakdown",
      resultClass = TaskBreakdownResult::class.java,
      exampleInstance = exampleInstance,
      prompt = prompt,
      model = planTaskSettings.model ?: this.defaultModel,
      parsingModel = this.parsingModel,
      temperature = this.temperature,
      describer = describer,
      parserPrompt = parserPrompt
    )
  }

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

  companion object {
    var exampleInstance = TaskBreakdownResult(
      tasksByID = mapOf(
        "1" to CommandAutoFixTaskConfigData(
          task_description = "Task 1",
          task_dependencies = listOf(),
          commands = listOf(
            CommandAutoFixTask.CommandWithWorkingDir(
              command = listOf("echo", "Hello, World!"),
              workingDir = "."
            )
          )
        ),
        "2" to FileModificationTaskConfigData(
          task_description = "Task 2",
          task_dependencies = listOf("1"),
          input_files = listOf("input2.txt"),
          output_files = listOf("output2.txt"),
        ),
        "3" to PlanningTaskConfigData(
          task_description = "Task 3",
          task_dependencies = listOf("2"),
        )
      ),
    )
  }
}