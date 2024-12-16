package com.simiacryptus.skyenet.apps.plan

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.simiacryptus.skyenet.apps.plan.tools.CommandAutoFixTask.CommandAutoFixTaskConfigData
import com.simiacryptus.skyenet.apps.plan.tools.plan.ForeachTask.ForeachTaskConfigData
import com.simiacryptus.skyenet.apps.plan.tools.online.SimpleGoogleSearchTask.GoogleSearchTaskConfigData
import com.simiacryptus.skyenet.apps.plan.tools.plan.PlanningTask.PlanningTaskConfigData
import com.simiacryptus.skyenet.apps.plan.tools.RunShellCommandTask.RunShellCommandTaskConfigData
import com.simiacryptus.skyenet.apps.plan.tools.file.CodeOptimizationTask.CodeOptimizationTaskConfigData
import com.simiacryptus.skyenet.apps.plan.tools.file.CodeReviewTask.CodeReviewTaskConfigData
import com.simiacryptus.skyenet.apps.plan.tools.file.DocumentationTask.DocumentationTaskConfigData
import com.simiacryptus.skyenet.apps.plan.tools.file.FileModificationTask.FileModificationTaskConfigData
import com.simiacryptus.skyenet.apps.plan.tools.file.InquiryTask.InquiryTaskConfigData
import com.simiacryptus.skyenet.apps.plan.tools.file.PerformanceAnalysisTask.PerformanceAnalysisTaskConfigData
import com.simiacryptus.skyenet.apps.plan.tools.file.RefactorTask.RefactorTaskConfigData
import com.simiacryptus.skyenet.apps.plan.tools.file.SecurityAuditTask.SecurityAuditTaskConfigData
import com.simiacryptus.skyenet.apps.plan.tools.file.TestGenerationTask.TestGenerationTaskConfigData
import com.simiacryptus.skyenet.apps.plan.tools.knowledge.EmbeddingSearchTask
import com.simiacryptus.skyenet.apps.plan.tools.knowledge.KnowledgeIndexingTask
import com.simiacryptus.skyenet.apps.plan.tools.knowledge.WebSearchAndIndexTask
import com.simiacryptus.skyenet.apps.plan.tools.*
import com.simiacryptus.skyenet.apps.plan.tools.file.*
import com.simiacryptus.skyenet.apps.plan.tools.online.GitHubSearchTask
import com.simiacryptus.skyenet.apps.plan.tools.online.SearchAndAnalyzeTask
import com.simiacryptus.skyenet.apps.plan.tools.online.SimpleGoogleSearchTask
import com.simiacryptus.skyenet.apps.plan.tools.online.WebFetchAndTransformTask
import com.simiacryptus.skyenet.apps.plan.tools.plan.ForeachTask
import com.simiacryptus.skyenet.apps.plan.tools.plan.PlanningTask
import com.simiacryptus.util.DynamicEnum
import com.simiacryptus.util.DynamicEnumDeserializer
import com.simiacryptus.util.DynamicEnumSerializer

@JsonDeserialize(using = TaskTypeDeserializer::class)
@JsonSerialize(using = TaskTypeSerializer::class)
class TaskType<out T : TaskConfigBase, out U : TaskSettingsBase>(
  name: String,
  val taskDataClass: Class<out T>,
  val taskSettingsClass: Class<out U>,
) : DynamicEnum<TaskType<*, *>>(name) {
  companion object {

    private val taskConstructors =
      mutableMapOf<TaskType<*, *>, (PlanSettings, TaskConfigBase?) -> AbstractTask<out TaskConfigBase>>()

    val TaskPlanning = TaskType("TaskPlanning", PlanningTaskConfigData::class.java, TaskSettingsBase::class.java)
    val Inquiry = TaskType("Inquiry", InquiryTaskConfigData::class.java, TaskSettingsBase::class.java)
    val Search = TaskType("Search", FileSearchTask.SearchTaskConfigData::class.java, TaskSettingsBase::class.java)
    val EmbeddingSearch = TaskType("EmbeddingSearch", EmbeddingSearchTask.EmbeddingSearchTaskConfigData::class.java, TaskSettingsBase::class.java)
    val FileModification = TaskType("FileModification", FileModificationTaskConfigData::class.java, TaskSettingsBase::class.java)
    val Documentation = TaskType("Documentation", DocumentationTaskConfigData::class.java, TaskSettingsBase::class.java)
    val CodeReview = TaskType("CodeReview", CodeReviewTaskConfigData::class.java, TaskSettingsBase::class.java)
    val TestGeneration = TaskType("TestGeneration", TestGenerationTaskConfigData::class.java, TaskSettingsBase::class.java)
    val Optimization = TaskType("Optimization", CodeOptimizationTaskConfigData::class.java, TaskSettingsBase::class.java)
    val SecurityAudit = TaskType("SecurityAudit", SecurityAuditTaskConfigData::class.java, TaskSettingsBase::class.java)
    val PerformanceAnalysis = TaskType("PerformanceAnalysis", PerformanceAnalysisTaskConfigData::class.java, TaskSettingsBase::class.java)
    val RefactorTask = TaskType("RefactorTask", RefactorTaskConfigData::class.java, TaskSettingsBase::class.java)
    val RunShellCommand = TaskType("RunShellCommand", RunShellCommandTaskConfigData::class.java, TaskSettingsBase::class.java)
    val CommandAutoFix = TaskType("CommandAutoFix", CommandAutoFixTaskConfigData::class.java, CommandAutoFixTask.CommandAutoFixTaskSettings::class.java)
    val ForeachTask = TaskType("ForeachTask", ForeachTaskConfigData::class.java, TaskSettingsBase::class.java)
    val GitHubSearch = TaskType("GitHubSearch", GitHubSearchTask.GitHubSearchTaskConfigData::class.java, TaskSettingsBase::class.java)
    val GoogleSearch = TaskType("GoogleSearch", GoogleSearchTaskConfigData::class.java, TaskSettingsBase::class.java)
    val WebFetchAndTransform =
      TaskType("WebFetchAndTransform", WebFetchAndTransformTask.WebFetchAndTransformTaskConfigData::class.java, TaskSettingsBase::class.java)
    val KnowledgeIndexing = TaskType("KnowledgeIndexing", KnowledgeIndexingTask.KnowledgeIndexingTaskConfigData::class.java, TaskSettingsBase::class.java)
    val WebSearchAndIndex = TaskType("WebSearchAndIndex", WebSearchAndIndexTask.WebSearchAndIndexTaskConfigData::class.java, TaskSettingsBase::class.java)
    val SeleniumSession = TaskType("SeleniumSession", SeleniumSessionTask.SeleniumSessionTaskConfigData::class.java, TaskSettingsBase::class.java)
    val CommandSession = TaskType("CommandSession", CommandSessionTask.CommandSessionTaskConfigData::class.java, TaskSettingsBase::class.java)
    val SearchAndAnalyze = TaskType("SearchAndAnalyze", SearchAndAnalyzeTask.SearchAndAnalyzeTaskConfigData::class.java, TaskSettingsBase::class.java)

    init {
      registerConstructor(CommandAutoFix) { settings, task -> CommandAutoFixTask(settings, task) }
      registerConstructor(Inquiry) { settings, task -> InquiryTask(settings, task) }
      registerConstructor(Search) { settings, task -> FileSearchTask(settings, task) }
      registerConstructor(SearchAndAnalyze) { settings, task -> SearchAndAnalyzeTask(settings, task) }
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
      registerConstructor(GoogleSearch) { settings, task -> SimpleGoogleSearchTask(settings, task) }
      registerConstructor(WebFetchAndTransform) { settings, task -> WebFetchAndTransformTask(settings, task) }
      registerConstructor(KnowledgeIndexing) { settings, task -> KnowledgeIndexingTask(settings, task) }
      registerConstructor(WebSearchAndIndex) { settings, task -> WebSearchAndIndexTask(settings, task) }
      registerConstructor(SeleniumSession) { settings, task -> SeleniumSessionTask(settings, task) }
      registerConstructor(CommandSession) { settings, task -> CommandSessionTask(settings, task) }
    }

    private fun <T : TaskConfigBase, U : TaskSettingsBase> registerConstructor(
      taskType: TaskType<T, U>,
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
      taskType: TaskType<*, *>,
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

    fun valueOf(name: String): TaskType<*, *> = valueOf(TaskType::class.java, name)
    private fun register(taskType: TaskType<*, *>) = register(TaskType::class.java, taskType)
  }
}

class TaskTypeSerializer : DynamicEnumSerializer<TaskType<*, *>>(TaskType::class.java)
class TaskTypeDeserializer : DynamicEnumDeserializer<TaskType<*, *>>(TaskType::class.java)