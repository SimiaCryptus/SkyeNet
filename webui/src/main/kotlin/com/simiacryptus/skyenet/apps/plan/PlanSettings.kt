package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.jopenai.models.OpenAITextModel
import com.simiacryptus.skyenet.apps.plan.PlanUtil.isWindows

data class PlanSettings(
    var model: OpenAITextModel,
    val parsingModel: OpenAITextModel,
    val command: List<String> = listOf(if (isWindows) "powershell" else "bash"),
    var temperature: Double = 0.2,
    val budget: Double = 2.0,
    var taskPlanningEnabled: Boolean = false,
    var shellCommandTaskEnabled: Boolean = false,
    var documentationEnabled: Boolean = false,
    var fileModificationEnabled: Boolean = true,
    var inquiryEnabled: Boolean = true,
    var codeReviewEnabled: Boolean = false,
    var testGenerationEnabled: Boolean = false,
    var optimizationEnabled: Boolean = false,
    var securityAuditEnabled: Boolean = false,
    var performanceAnalysisEnabled: Boolean = false,
    var refactorTaskEnabled: Boolean = false,
    var foreachTaskEnabled: Boolean = false,
    var autoFix: Boolean = false,
    var enableCommandAutoFix: Boolean = false,
    var commandAutoFixCommands: List<String>? = listOf(),
    val env: Map<String, String>? = mapOf(),
    val workingDir: String? = ".",
    val language: String? = if (isWindows) "powershell" else "bash",
)