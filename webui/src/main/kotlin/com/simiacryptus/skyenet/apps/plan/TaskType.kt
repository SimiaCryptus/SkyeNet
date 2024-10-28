package com.simiacryptus.skyenet.apps.plan

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.skyenet.apps.plan.AbstractTask.TaskState
import com.simiacryptus.skyenet.apps.plan.CommandAutoFixTask.CommandAutoFixTaskData
import com.simiacryptus.skyenet.apps.plan.ForeachTask.ForeachTaskData
import com.simiacryptus.skyenet.apps.plan.PlanningTask.PlanningTaskData
import com.simiacryptus.skyenet.apps.plan.RunShellCommandTask.RunShellCommandTaskData
import com.simiacryptus.skyenet.apps.plan.file.*
import com.simiacryptus.skyenet.apps.plan.file.CodeOptimizationTask.CodeOptimizationTaskData
import com.simiacryptus.skyenet.apps.plan.file.CodeReviewTask.CodeReviewTaskData
import com.simiacryptus.skyenet.apps.plan.file.DocumentationTask.DocumentationTaskData
import com.simiacryptus.skyenet.apps.plan.file.FileModificationTask.FileModificationTaskData
import com.simiacryptus.skyenet.apps.plan.file.GoogleSearchTask.GoogleSearchTaskData
import com.simiacryptus.skyenet.apps.plan.file.InquiryTask.InquiryTaskData
import com.simiacryptus.skyenet.apps.plan.file.PerformanceAnalysisTask.PerformanceAnalysisTaskData
import com.simiacryptus.skyenet.apps.plan.file.RefactorTask.RefactorTaskData
import com.simiacryptus.skyenet.apps.plan.file.SecurityAuditTask.SecurityAuditTaskData
import com.simiacryptus.skyenet.apps.plan.file.TestGenerationTask.TestGenerationTaskData
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
        val Search = TaskType("Search", SearchTask.SearchTaskData::class.java)
        val EmbeddingSearch = TaskType("EmbeddingSearch", EmbeddingSearchTask.EmbeddingSearchTaskData::class.java)
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
        val GitHubSearch = TaskType("GitHubSearch", GitHubSearchTask.GitHubSearchTaskData::class.java)
        val GoogleSearch = TaskType("GoogleSearch", GoogleSearchTaskData::class.java)
        val WebFetchAndTransform = TaskType("WebFetchAndTransform", WebFetchAndTransformTask.WebFetchAndTransformTaskData::class.java)

        init {
            registerConstructor(CommandAutoFix) { settings, task -> CommandAutoFixTask(settings, task) }
            registerConstructor(Inquiry) { settings, task -> InquiryTask(settings, task) }
            registerConstructor(Search) { settings, task -> SearchTask(settings, task) }
            registerConstructor(EmbeddingSearch) { settings, task -> EmbeddingSearchTask(settings, task) }
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
            registerConstructor(GitHubSearch) { settings, task -> GitHubSearchTask(settings, task) }
            registerConstructor(GoogleSearch) { settings, task -> GoogleSearchTask(settings, task) }
            registerConstructor(WebFetchAndTransform) { settings, task -> WebFetchAndTransformTask(settings, task) }
        }

        private fun <T : PlanTaskBase> registerConstructor(
            taskType: TaskType<T>,
            constructor: (PlanSettings, T?) -> AbstractTask<T>
        ) {
            taskConstructors[taskType] = { settings: PlanSettings, task: PlanTaskBase? ->
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

@JsonTypeIdResolver(PlanTaskTypeIdResolver::class)
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "task_type")
abstract class PlanTaskBase(
    @Description("An enumeration indicating the type of task to be executed. Must be a single value from the TaskType enum.")
    val task_type: String? = null,
    @Description("A detailed description of the specific task to be performed, including its role in the overall plan and its dependencies on other tasks.")
    var task_description: String? = null,
    @Description("A list of IDs of tasks that must be completed before this task can be executed. This defines upstream dependencies ensuring proper task order and information flow.")
    var task_dependencies: List<String>? = null,
    @Description("The current execution state of the task. Important for coordinating task execution and managing dependencies.")
    var state: TaskState? = null
)

class PlanTaskTypeIdResolver : TypeIdResolverBase() {
    override fun idFromValue(value: Any) = when (value) {
        is PlanTaskBase -> if (value.task_type != null) {
            value.task_type
        } else {
            throw IllegalArgumentException("Unknown task type")
        }

        else -> throw IllegalArgumentException("Unexpected value type: ${value.javaClass}")
    }

    override fun idFromValueAndType(value: Any, suggestedType: Class<*>): String {
        return idFromValue(value)
    }

    override fun typeFromId(context: DatabindContext, id: String): JavaType {
        val taskType = TaskType.valueOf(id.replace(" ", ""))
        val subType = context.constructType(taskType.taskDataClass)
        return subType
    }

    override fun getMechanism(): JsonTypeInfo.Id {
        return JsonTypeInfo.Id.CUSTOM
    }
}

class TaskTypeSerializer : DynamicEnumSerializer<TaskType<*>>(TaskType::class.java)
class TaskTypeDeserializer : DynamicEnumDeserializer<TaskType<*>>(TaskType::class.java)