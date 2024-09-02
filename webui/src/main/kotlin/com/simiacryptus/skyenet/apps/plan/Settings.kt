package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.jopenai.models.OpenAITextModel
import com.simiacryptus.skyenet.apps.plan.PlanCoordinator.TaskBreakdownResult
import com.simiacryptus.skyenet.core.actors.ParsedActor

data class Settings(
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
    val language: String = if (PlanCoordinator.isWindows) "powershell" else "bash",
) {
    fun getImpl(planTask: PlanTask): AbstractTask {
        return when (planTask.taskType) {
            TaskType.TaskPlanning -> if (taskPlanningEnabled) PlanningTask(this, planTask) else throw DisabledTaskException(planTask.taskType)
            TaskType.Documentation -> if (documentationEnabled) DocumentationTask(this, planTask) else throw DisabledTaskException(planTask.taskType)
            TaskType.FileModification -> if (fileModificationEnabled) FileModificationTask(this, planTask) else throw DisabledTaskException(planTask.taskType)
            TaskType.RunShellCommand -> if (shellCommandTaskEnabled) RunShellCommandTask(this, planTask) else throw DisabledTaskException(planTask.taskType)
            TaskType.CommandAutoFix -> if (enableCommandAutoFix) CommandAutoFixTask(this, planTask) else throw DisabledTaskException(planTask.taskType)
            TaskType.Inquiry -> if (inquiryEnabled) InquiryTask(this, planTask) else throw DisabledTaskException(planTask.taskType)
            TaskType.CodeReview -> if (codeReviewEnabled) CodeReviewTask(this, planTask) else throw DisabledTaskException(planTask.taskType)
            TaskType.TestGeneration -> if (testGenerationEnabled) TestGenerationTask(this, planTask) else throw DisabledTaskException(planTask.taskType)
            TaskType.Optimization -> if (optimizationEnabled) CodeOptimizationTask(this, planTask) else throw DisabledTaskException(planTask.taskType)
            TaskType.SecurityAudit -> if (securityAuditEnabled) SecurityAuditTask(
                this,
                planTask
            ) else throw DisabledTaskException(planTask.taskType)

            TaskType.PerformanceAnalysis -> if (performanceAnalysisEnabled) PerformanceAnalysisTask(
                this,
                planTask
            ) else throw DisabledTaskException(planTask.taskType)

            TaskType.RefactorTask -> if (refactorTaskEnabled) RefactorTask(
                this,
                planTask
            ) else throw DisabledTaskException(planTask.taskType)
            TaskType.ForeachTask -> if (foreachTaskEnabled) ForeachTask(
                this,
                planTask
            ) else throw DisabledTaskException(planTask.taskType)
            else -> throw RuntimeException("Unknown task type: ${planTask.taskType}")
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
                |${getAvailableTaskTypes().joinToString("\n") { "* ${it.promptSegment()}" }}
                |
                |${if (taskPlanningEnabled) "Do not start your plan with a plan to plan!\n" else ""}
                """.trimMargin(),
        model = this.model,
        parsingModel = this.parsingModel,
        temperature = this.temperature,
    )

    private fun getAvailableTaskTypes(): List<AbstractTask> = TaskType.values().filter {
        when (it) {
            TaskType.TaskPlanning -> this.taskPlanningEnabled
            TaskType.RunShellCommand -> this.shellCommandTaskEnabled
            TaskType.Documentation -> this.documentationEnabled
            TaskType.FileModification -> this.fileModificationEnabled
            TaskType.Inquiry -> this.inquiryEnabled
            TaskType.CodeReview -> this.codeReviewEnabled
            TaskType.TestGeneration -> this.testGenerationEnabled
            TaskType.Optimization -> this.optimizationEnabled
            TaskType.CommandAutoFix -> this.enableCommandAutoFix
            TaskType.SecurityAudit -> this.securityAuditEnabled
            TaskType.PerformanceAnalysis -> this.performanceAnalysisEnabled
            TaskType.RefactorTask -> this.refactorTaskEnabled
            TaskType.ForeachTask -> this.foreachTaskEnabled
        }
    }.map { this.getImpl(PlanTask(taskType = it)) }
}
class DisabledTaskException(taskType: TaskType) : Exception("Task type $taskType is disabled")