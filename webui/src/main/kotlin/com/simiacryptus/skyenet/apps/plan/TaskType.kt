package com.simiacryptus.skyenet.apps.plan

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.simiacryptus.util.DynamicEnum
import com.simiacryptus.util.DynamicEnumDeserializer
import com.simiacryptus.util.DynamicEnumSerializer

@JsonDeserialize(using = TaskTypeDeserializer::class)
@JsonSerialize(using = TaskTypeSerializer::class)
class TaskType(name: String) : DynamicEnum<TaskType>(name) {
    companion object {
        private val taskConstructors = mutableMapOf<TaskType, (PlanSettings, PlanningTask.PlanTask) -> AbstractTask>()

        val TaskPlanning = TaskType("TaskPlanning").also { register(it) }
        val Inquiry = TaskType("Inquiry").also { register(it) }
        val FileModification = TaskType("FileModification").also { register(it) }
        val Documentation = TaskType("Documentation").also { register(it) }
        val RunShellCommand = TaskType("RunShellCommand").also { register(it) }
        val CommandAutoFix = TaskType("CommandAutoFix").also { register(it) }
        val CodeReview = TaskType("CodeReview").also { register(it) }
        val TestGeneration = TaskType("TestGeneration").also { register(it) }
        val Optimization = TaskType("Optimization").also { register(it) }
        val SecurityAudit = TaskType("SecurityAudit").also { register(it) }
        val PerformanceAnalysis = TaskType("PerformanceAnalysis").also { register(it) }
        val RefactorTask = TaskType("RefactorTask").also { register(it) }
        val ForeachTask = TaskType("ForeachTask").also { register(it) }

        init {
            registerConstructor(TaskPlanning) { settings, task -> PlanningTask(settings, task) }
            registerConstructor(Inquiry) { settings, task -> InquiryTask(settings, task) }
            registerConstructor(FileModification) { settings, task -> FileModificationTask(settings, task) }
            registerConstructor(Documentation) { settings, task -> DocumentationTask(settings, task) }
            registerConstructor(RunShellCommand) { settings, task -> RunShellCommandTask(settings, task) }
            registerConstructor(CommandAutoFix) { settings, task -> CommandAutoFixTask(settings, task) }
            registerConstructor(CodeReview) { settings, task -> CodeReviewTask(settings, task) }
            registerConstructor(TestGeneration) { settings, task -> TestGenerationTask(settings, task) }
            registerConstructor(Optimization) { settings, task -> CodeOptimizationTask(settings, task) }
            registerConstructor(SecurityAudit) { settings, task -> SecurityAuditTask(settings, task) }
            registerConstructor(PerformanceAnalysis) { settings, task -> PerformanceAnalysisTask(settings, task) }
            registerConstructor(RefactorTask) { settings, task -> RefactorTask(settings, task) }
            registerConstructor(ForeachTask) { settings, task -> ForeachTask(settings, task) }
        }

        private fun registerConstructor(
            taskType: TaskType,
            constructor: (PlanSettings, PlanningTask.PlanTask) -> AbstractTask
        ) {
            taskConstructors[taskType] = constructor
        }

        fun values() = values(TaskType::class.java)
        fun getImpl(planSettings: PlanSettings, planTask: PlanningTask.PlanTask): AbstractTask {
            val taskType = planTask.taskType ?: throw RuntimeException("Task type is null")
            if (!planSettings.getTaskSettings(taskType).enabled) {
                throw DisabledTaskException(taskType)
            }
            val constructor =
                taskConstructors[taskType] ?: throw RuntimeException("Unknown task type: ${taskType.name}")
            return constructor(planSettings, planTask)
        }

        fun getAvailableTaskTypes(planSettings: PlanSettings) = values().filter {
            planSettings.getTaskSettings(it).enabled
        }.map { getImpl(planSettings, PlanningTask.PlanTask(taskType = it)) }

        fun valueOf(name: String): TaskType = valueOf(TaskType::class.java, name)
        private fun register(taskType: TaskType) = register(TaskType::class.java, taskType)
    }
}

class TaskTypeSerializer : DynamicEnumSerializer<TaskType>(TaskType::class.java)
class TaskTypeDeserializer : DynamicEnumDeserializer<TaskType>(TaskType::class.java)