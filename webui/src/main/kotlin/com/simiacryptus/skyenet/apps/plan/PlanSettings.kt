package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.jopenai.models.OpenAITextModel
import com.simiacryptus.skyenet.apps.plan.PlanUtil.isWindows

data class PlanSettings(
    var model: OpenAITextModel,
    val parsingModel: OpenAITextModel,
    val command: List<String>,
    var temperature: Double = 0.2,
    val budget: Double = 2.0,
    var taskPlanningEnabled: Boolean = false,
    var shellCommandTaskEnabled: Boolean = true,
    var documentationEnabled: Boolean = true,
    var fileModificationEnabled: Boolean = true,
    var inquiryEnabled: Boolean = true,
    var codeReviewEnabled: Boolean = true,
    var testGenerationEnabled: Boolean = true,
    var optimizationEnabled: Boolean = true,
    var securityAuditEnabled: Boolean = true,
    var performanceAnalysisEnabled: Boolean = true,
    var refactorTaskEnabled: Boolean = true,
    var foreachTaskEnabled: Boolean = true,
    var autoFix: Boolean = false,
    var enableCommandAutoFix: Boolean = false,
    var commandAutoFixCommands: List<String> = listOf(),
    val env: Map<String, String> = mapOf(),
    val workingDir: String = ".",
    val language: String = if (isWindows) "powershell" else "bash",
)