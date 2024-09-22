package com.simiacryptus.skyenet.apps.plan

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.simiacryptus.skyenet.apps.plan.AbstractTask.PlanTaskBaseInterface
import com.simiacryptus.skyenet.apps.plan.ForeachTask.ForeachTaskData
import com.simiacryptus.skyenet.apps.plan.PlanningTask.PlanTask
import com.simiacryptus.skyenet.apps.plan.RunShellCommandTask.ExecutionTaskInterface
import com.simiacryptus.util.DynamicEnum
import com.simiacryptus.util.DynamicEnumDeserializer
import com.simiacryptus.util.DynamicEnumSerializer

@JsonDeserialize(using = TaskTypeDeserializer::class)
@JsonSerialize(using = TaskTypeSerializer::class)
class TaskType<out T : PlanTaskBaseInterface>(
    name: String,
    val taskDataClass: Class<out T>
) : DynamicEnum<TaskType<*>>(name) {
    companion object {

        private val taskConstructors =
            mutableMapOf<TaskType<*>, (PlanSettings, PlanTaskBaseInterface?) -> AbstractTask<out PlanTaskBaseInterface>>()

        val TaskPlanning = TaskType<PlanTaskBaseInterface>("TaskPlanning", PlanTask::class.java)
        val Inquiry = TaskType<PlanTaskBaseInterface>("Inquiry", PlanTask::class.java)
        val FileModification = TaskType<PlanTaskBaseInterface>("FileModification", PlanTask::class.java)
        val Documentation = TaskType<PlanTaskBaseInterface>("Documentation", PlanTask::class.java)
        val CodeReview = TaskType<PlanTaskBaseInterface>("CodeReview", PlanTask::class.java)
        val TestGeneration = TaskType<PlanTaskBaseInterface>("TestGeneration", PlanTask::class.java)
        val Optimization = TaskType<PlanTaskBaseInterface>("Optimization", PlanTask::class.java)
        val SecurityAudit = TaskType<PlanTaskBaseInterface>("SecurityAudit", PlanTask::class.java)
        val PerformanceAnalysis = TaskType<PlanTaskBaseInterface>("PerformanceAnalysis", PlanTask::class.java)
        val RefactorTask = TaskType<PlanTaskBaseInterface>("RefactorTask", PlanTask::class.java)
        val RunShellCommand = TaskType<ExecutionTaskInterface>("RunShellCommand", PlanTask::class.java)
        val CommandAutoFix = TaskType<ExecutionTaskInterface>("CommandAutoFix", PlanTask::class.java)
        val ForeachTask = TaskType("ForeachTask", ForeachTaskData::class.java)

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
            constructor: (PlanSettings, T?) -> AbstractTask<out PlanTaskBaseInterface>
        ) {
            taskConstructors[taskType] = { settings : PlanSettings, task : T? -> constructor(settings, task) }
                    as (PlanSettings, PlanTaskBaseInterface?) -> AbstractTask<out PlanTaskBaseInterface>
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