package com.simiacryptus.skyenet.apps.plan

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.simiacryptus.skyenet.apps.plan.AbstractTask.PlanTaskBase
import com.simiacryptus.skyenet.apps.plan.CodeOptimizationTask.CodeOptimizationTaskData
import com.simiacryptus.skyenet.apps.plan.CodeReviewTask.CodeReviewTaskData
import com.simiacryptus.skyenet.apps.plan.CommandAutoFixTask.CommandAutoFixTaskData
import com.simiacryptus.skyenet.apps.plan.DocumentationTask.DocumentationTaskData
import com.simiacryptus.skyenet.apps.plan.FileModificationTask.FileModificationTaskData
import com.simiacryptus.skyenet.apps.plan.ForeachTask.ForeachTaskData
import com.simiacryptus.skyenet.apps.plan.InquiryTask.InquiryTaskData
import com.simiacryptus.skyenet.apps.plan.PerformanceAnalysisTask.PerformanceAnalysisTaskData
import com.simiacryptus.skyenet.apps.plan.PlanningTask.PlanningTaskData
import com.simiacryptus.skyenet.apps.plan.RefactorTask.RefactorTaskData
import com.simiacryptus.skyenet.apps.plan.RunShellCommandTask.RunShellCommandTaskData
import com.simiacryptus.skyenet.apps.plan.SecurityAuditTask.SecurityAuditTaskData
import com.simiacryptus.skyenet.apps.plan.TestGenerationTask.TestGenerationTaskData
import com.simiacryptus.util.DynamicEnum
import com.simiacryptus.util.DynamicEnumDeserializer
import com.simiacryptus.util.DynamicEnumSerializer

@JsonDeserialize(using = TaskTypeDeserializer::class)
@JsonSerialize(using = TaskTypeSerializer::class)
class TaskType<out T : PlanTaskBase>(
    name: String,
    val taskDataClass: Class<out T>
) : DynamicEnum<TaskType<*>>(name) {
    companion object {

        private val taskConstructors =
            mutableMapOf<TaskType<*>, (PlanSettings, PlanTaskBase?) -> AbstractTask<out PlanTaskBase>>()

        val TaskPlanning = TaskType("TaskPlanning", PlanningTaskData::class.java)
        val Inquiry = TaskType("Inquiry", InquiryTaskData::class.java)
        val FileModification = TaskType("FileModification", FileModificationTaskData::class.java)
        val Documentation = TaskType("Documentation", DocumentationTaskData::class.java)
        val CodeReview = TaskType("CodeReview", CodeReviewTaskData::class.java)
        val TestGeneration = TaskType("TestGeneration", TestGenerationTaskData::class.java)
        val Optimization = TaskType("Optimization", CodeOptimizationTaskData::class.java)
        val SecurityAudit = TaskType("SecurityAudit", SecurityAuditTaskData::class.java)
        val PerformanceAnalysis = TaskType("PerformanceAnalysis", PerformanceAnalysisTaskData::class.java)
        val RefactorTask = TaskType("RefactorTask", RefactorTaskData::class.java)
        val RunShellCommand = TaskType("RunShellCommand", RunShellCommandTaskData::class.java)
        val CommandAutoFix = TaskType("CommandAutoFix", CommandAutoFixTaskData::class.java)
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

        private fun <T : PlanTaskBase> registerConstructor(
            taskType: TaskType<T>,
            constructor: (PlanSettings, T?) -> AbstractTask<T>
        ) {
            taskConstructors[taskType] = { settings : PlanSettings, task : PlanTaskBase? ->
                val taskDataClass = taskType.taskDataClass
                if (task != null && taskDataClass.isAssignableFrom(task.javaClass)) {
                    throw RuntimeException("Task type mismatch: ${taskType.name} != ${task.javaClass.name}")
                }
                constructor(settings, task as T?)
            }
            register(taskType)
        }

        fun values() = values(TaskType::class.java)
        fun getImpl(
            planSettings: PlanSettings,
            planTask: PlanTaskBase?
        ): AbstractTask<out PlanTaskBase> {
            val taskType = planTask?.task_type?.let { valueOf(it) }
                ?: throw RuntimeException("Task type not specified")
            return getImpl(planSettings, taskType, planTask)
        }

        fun getImpl(
            planSettings: PlanSettings,
            taskType: TaskType<*>,
            planTask: PlanTaskBase? = null
        ): AbstractTask<out PlanTaskBase> {
            if (!planSettings.getTaskSettings(taskType).enabled) {
                throw DisabledTaskException(taskType)
            }
            val constructor =
                taskConstructors[taskType] ?: throw RuntimeException("Unknown task type: ${taskType.name}")
            return constructor(planSettings, planTask)
        }

        fun getAvailableTaskTypes(planSettings: PlanSettings) = values().filter {
            planSettings.getTaskSettings(it).enabled
        }

        fun valueOf(name: String): TaskType<*> = valueOf(TaskType::class.java, name)
        private fun register(taskType: TaskType<*>) = register(TaskType::class.java, taskType)
    }
}

class TaskTypeSerializer : DynamicEnumSerializer<TaskType<*>>(TaskType::class.java)
class TaskTypeDeserializer : DynamicEnumDeserializer<TaskType<*>>(TaskType::class.java)