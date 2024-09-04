package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.jopenai.models.OpenAITextModel
import com.simiacryptus.skyenet.apps.plan.PlanUtil.isWindows

data class PlanSettings(
    val model: OpenAITextModel,
    val parsingModel: OpenAITextModel,
    val command: List<String>,
    val temperature: Double = 0.2,
    val budget: Double = 2.0,
    val taskPlanningEnabled: Boolean = false,
    val shellCommandTaskEnabled: Boolean = true,
    val documentationEnabled: Boolean = true,
    val fileModificationEnabled: Boolean = true,
    val inquiryEnabled: Boolean = true,
    val codeReviewEnabled: Boolean = true,
    val testGenerationEnabled: Boolean = true,
    val optimizationEnabled: Boolean = true,
    val securityAuditEnabled: Boolean = true,
    val performanceAnalysisEnabled: Boolean = true,
    val refactorTaskEnabled: Boolean = true,
    val foreachTaskEnabled: Boolean = true,
    val autoFix: Boolean = false,
    val enableCommandAutoFix: Boolean = false,
    var commandAutoFixCommands: List<String> = listOf(),
    val env: Map<String, String> = mapOf(),
    val workingDir: String = ".",
    val language: String = if (isWindows) "powershell" else "bash",
)