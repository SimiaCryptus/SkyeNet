package com.simiacryptus.skyenet.apps.plan

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.simiacryptus.skyenet.apps.plan.AbstractTask.PlanTaskBaseInterface
import com.simiacryptus.skyenet.apps.plan.ForeachTask.ForeachTaskInterface
import com.simiacryptus.skyenet.apps.plan.RunShellCommandTask.ExecutionTaskInterface
import com.simiacryptus.util.DynamicEnum
import com.simiacryptus.util.DynamicEnumDeserializer
import com.simiacryptus.util.DynamicEnumSerializer

@JsonDeserialize(using = TaskTypeDeserializer::class)
@JsonSerialize(using = TaskTypeSerializer::class)
class TaskType<out T : PlanTaskBaseInterface>(name: String) : DynamicEnum<TaskType<*>>(name) {
    companion object {

        private val taskConstructors =
            mutableMapOf<TaskType<*>, (PlanSettings, PlanTaskBaseInterface?) -> AbstractTask<out PlanTaskBaseInterface>>()

        val TaskPlanning = TaskType<PlanTaskBaseInterface>("TaskPlanning")
        val Inquiry = TaskType<PlanTaskBaseInterface>("Inquiry")
        val FileModification = TaskType<PlanTaskBaseInterface>("FileModification")
        val Documentation = TaskType<PlanTaskBaseInterface>("Documentation")
        val CodeReview = TaskType<PlanTaskBaseInterface>("CodeReview")
        val TestGeneration = TaskType<PlanTaskBaseInterface>("TestGeneration")
        val Optimization = TaskType<PlanTaskBaseInterface>("Optimization")
        val SecurityAudit = TaskType<PlanTaskBaseInterface>("SecurityAudit")
        val PerformanceAnalysis = TaskType<PlanTaskBaseInterface>("PerformanceAnalysis")
        val RefactorTask = TaskType<PlanTaskBaseInterface>("RefactorTask")
        val RunShellCommand = TaskType<ExecutionTaskInterface>("RunShellCommand")
        val CommandAutoFix = TaskType<ExecutionTaskInterface>("CommandAutoFix")
        val ForeachTask = TaskType<ForeachTaskInterface>("ForeachTask")

        init {
            registerConstructor(CommandAutoFix) { settings, task -> CommandAutoFixTask(settings, task) }
            registerConstructor(Inquiry) { settings, task -> InquiryTask(settings, task) }
            registerConstructor(FileModification) { settings, task -> FileModificationTask(settings, task) }
            registerConstructor(Documentation) { settings, task -> DocumentationTask(settings, task) }
            registerConstructor(RunShellCommand) { settings, task -> RunShellCommandTask(settings, task) }
            registerConstructor(CodeReview) { settings, task -> CodeReviewTask(settings, task) }
            registerConstructor(TestGeneration) { settings, task -> TestGenerationTask(settings, task) }
            registerConstructor(Optimization) { settings, task -> CodeOptimizationTask(settings, task) }
            registerConstructor(SecurityAudit) { settings, task -> SecurityAuditTask(settings, task) }
            registerConstructor(PerformanceAnalysis) { settings, task -> PerformanceAnalysisTask(settings, task) }
            registerConstructor(RefactorTask) { settings, task -> RefactorTask(settings, task) }
            registerConstructor(ForeachTask) { settings, task -> ForeachTask(settings, task) }
            registerConstructor(TaskPlanning) { settings, task -> PlanningTask(settings, task) }
        }

        private fun <T : PlanTaskBaseInterface> registerConstructor(
            taskType: TaskType<T>,
            constructor: (PlanSettings, T) -> AbstractTask<out PlanTaskBaseInterface>
        ) {
            taskConstructors[taskType] = { settings, task -> constructor(settings, task as T) }
            register(taskType)
        }

        fun values() = values(TaskType::class.java)
        fun getImpl(
            planSettings: PlanSettings,
            planTask: PlanTaskBaseInterface?
        ): AbstractTask<out PlanTaskBaseInterface> {
            val taskType = planTask?.task_type as TaskType<*>
            return getImpl(planSettings, taskType, planTask)
        }

        private fun getImpl(
            planSettings: PlanSettings,
            taskType: TaskType<*>,
            planTask: PlanTaskBaseInterface? = null
        ): AbstractTask<out PlanTaskBaseInterface> {
            if (!planSettings.getTaskSettings(taskType).enabled) {
                throw DisabledTaskException(taskType)
            }
            val constructor =
                taskConstructors[taskType] ?: throw RuntimeException("Unknown task type: ${taskType.name}")
            return constructor(planSettings, planTask)
        }

        fun getAvailableTaskTypes(planSettings: PlanSettings) = values().filter {
            planSettings.getTaskSettings(it).enabled
        }.map { getImpl(planSettings, it) }

        fun valueOf(name: String): TaskType<*> = valueOf(TaskType::class.java, name)
        private fun register(taskType: TaskType<*>) = register(TaskType::class.java, taskType)
    }
}

class TaskTypeSerializer : DynamicEnumSerializer<TaskType<*>>(TaskType::class.java)
class TaskTypeDeserializer : DynamicEnumDeserializer<TaskType<*>>(TaskType::class.java)