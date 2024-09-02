package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.jopenai.describe.Description

data class PlanTask(
    val description: String? = null,
    val taskType: TaskType? = null,
    var task_dependencies: List<String>? = null,
    val input_files: List<String>? = null,
    val output_files: List<String>? = null,
    var state: AbstractTask.TaskState? = null,
    @Description("Command and arguments (in list form) for the task")
    val command: List<String>? = null,
    @Description("Working directory for the command execution")
    val workingDir: String? = null
)