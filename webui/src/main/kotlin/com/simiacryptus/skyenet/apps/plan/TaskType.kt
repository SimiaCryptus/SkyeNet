package com.simiacryptus.skyenet.apps.plan

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.skyenet.apps.plan.AbstractTask.TaskState
import com.simiacryptus.skyenet.apps.plan.CommandAutoFixTask.CommandAutoFixTaskConfigData
import com.simiacryptus.skyenet.apps.plan.ForeachTask.ForeachTaskConfigData
import com.simiacryptus.skyenet.apps.plan.GoogleSearchTask.GoogleSearchTaskConfigData
import com.simiacryptus.skyenet.apps.plan.PlanningTask.PlanningTaskConfigData
import com.simiacryptus.skyenet.apps.plan.RunShellCommandTask.RunShellCommandTaskConfigData
import com.simiacryptus.skyenet.apps.plan.file.*
import com.simiacryptus.skyenet.apps.plan.file.CodeOptimizationTask.CodeOptimizationTaskConfigData
import com.simiacryptus.skyenet.apps.plan.file.CodeReviewTask.CodeReviewTaskConfigData
import com.simiacryptus.skyenet.apps.plan.file.DocumentationTask.DocumentationTaskConfigData
import com.simiacryptus.skyenet.apps.plan.file.FileModificationTask.FileModificationTaskConfigData
import com.simiacryptus.skyenet.apps.plan.file.InquiryTask.InquiryTaskConfigData
import com.simiacryptus.skyenet.apps.plan.file.PerformanceAnalysisTask.PerformanceAnalysisTaskConfigData
import com.simiacryptus.skyenet.apps.plan.file.RefactorTask.RefactorTaskConfigData
import com.simiacryptus.skyenet.apps.plan.file.SecurityAuditTask.SecurityAuditTaskConfigData
import com.simiacryptus.skyenet.apps.plan.file.TestGenerationTask.TestGenerationTaskConfigData
import com.simiacryptus.skyenet.apps.plan.knowledge.EmbeddingSearchTask
import com.simiacryptus.skyenet.apps.plan.knowledge.KnowledgeIndexingTask
import com.simiacryptus.skyenet.apps.plan.knowledge.WebSearchAndIndexTask
import com.simiacryptus.util.DynamicEnum
import com.simiacryptus.util.DynamicEnumDeserializer
import com.simiacryptus.util.DynamicEnumSerializer

@JsonDeserialize(using = TaskTypeDeserializer::class)
@JsonSerialize(using = TaskTypeSerializer::class)
class TaskType<out T : TaskConfigBase>(
  name: String,
  val taskDataClass: Class<out T>
) : DynamicEnum<TaskType<*>>(name) {
  companion object {

    private val taskConstructors =
      mutableMapOf<TaskType<*>, (PlanSettings, TaskConfigBase?) -> AbstractTask<out TaskConfigBase>>()

    val TaskPlanning = TaskType("TaskPlanning", PlanningTaskConfigData::class.java)
    val Inquiry = TaskType("Inquiry", InquiryTaskConfigData::class.java)
    val Search = TaskType("Search", SearchTask.SearchTaskConfigData::class.java)
    val EmbeddingSearch = TaskType("EmbeddingSearch", EmbeddingSearchTask.EmbeddingSearchTaskConfigData::class.java)
    val FileModification = TaskType("FileModification", FileModificationTaskConfigData::class.java)
    val Documentation = TaskType("Documentation", DocumentationTaskConfigData::class.java)
    val CodeReview = TaskType("CodeReview", CodeReviewTaskConfigData::class.java)
    val TestGeneration = TaskType("TestGeneration", TestGenerationTaskConfigData::class.java)
    val Optimization = TaskType("Optimization", CodeOptimizationTaskConfigData::class.java)
    val SecurityAudit = TaskType("SecurityAudit", SecurityAuditTaskConfigData::class.java)
    val PerformanceAnalysis = TaskType("PerformanceAnalysis", PerformanceAnalysisTaskConfigData::class.java)
    val RefactorTask = TaskType("RefactorTask", RefactorTaskConfigData::class.java)
    val RunShellCommand = TaskType("RunShellCommand", RunShellCommandTaskConfigData::class.java)
    val CommandAutoFix = TaskType("CommandAutoFix", CommandAutoFixTaskConfigData::class.java)
    val ForeachTask = TaskType("ForeachTask", ForeachTaskConfigData::class.java)
    val GitHubSearch = TaskType("GitHubSearch", GitHubSearchTask.GitHubSearchTaskConfigData::class.java)
    val GoogleSearch = TaskType("GoogleSearch", GoogleSearchTaskConfigData::class.java)
    val WebFetchAndTransform = TaskType("WebFetchAndTransform", WebFetchAndTransformTask.WebFetchAndTransformTaskConfigData::class.java)
    val KnowledgeIndexing = TaskType("KnowledgeIndexing", KnowledgeIndexingTask.KnowledgeIndexingTaskConfigData::class.java)
    val WebSearchAndIndex = TaskType("WebSearchAndIndex", WebSearchAndIndexTask.WebSearchAndIndexTaskConfigData::class.java)
    val SeleniumSession = TaskType("SeleniumSession", SeleniumSessionTask.SeleniumSessionTaskConfigData::class.java)
    val CommandSession = TaskType("CommandSession", CommandSessionTask.CommandSessionTaskConfigData::class.java)

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
      registerConstructor(KnowledgeIndexing) { settings, task -> KnowledgeIndexingTask(settings, task) }
      registerConstructor(WebSearchAndIndex) { settings, task -> WebSearchAndIndexTask(settings, task) }
      registerConstructor(SeleniumSession) { settings, task -> SeleniumSessionTask(settings, task) }
      registerConstructor(CommandSession) { settings, task -> CommandSessionTask(settings, task) }
    }

    private fun <T : TaskConfigBase> registerConstructor(
      taskType: TaskType<T>,
      constructor: (PlanSettings, T?) -> AbstractTask<T>
    ) {
      taskConstructors[taskType] = { settings: PlanSettings, task: TaskConfigBase? ->
        constructor(settings, task as T?)
      }
      register(taskType)
    }

    fun values() = values(TaskType::class.java)
    fun getImpl(
      planSettings: PlanSettings,
      planTask: TaskConfigBase?
    ) = getImpl(planSettings, planTask?.task_type?.let { valueOf(it) } ?: throw RuntimeException("Task type not specified"), planTask)

    fun getImpl(
      planSettings: PlanSettings,
      taskType: TaskType<*>,
      planTask: TaskConfigBase? = null
    ): AbstractTask<out TaskConfigBase> {
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
abstract class TaskConfigBase(
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
    is TaskConfigBase -> if (value.task_type != null) {
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