package com.simiacryptus.skyenet.apps.plan


class TaskType(val name: String) {
    companion object {
        private val registry = mutableMapOf<String, TaskType>()
        private val taskConstructors = mutableMapOf<TaskType, (PlanSettings, PlanningTask.PlanTask) -> AbstractTask>()
        val TaskPlanning = TaskType("TaskPlanning")
        val Inquiry = TaskType("Inquiry")
        val FileModification = TaskType("FileModification")
        val Documentation = TaskType("Documentation")
        val RunShellCommand = TaskType("RunShellCommand")
        val CommandAutoFix = TaskType("CommandAutoFix")
        val CodeReview = TaskType("CodeReview")
        val TestGeneration = TaskType("TestGeneration")
        val Optimization = TaskType("Optimization")
        val SecurityAudit = TaskType("SecurityAudit")
        val PerformanceAnalysis = TaskType("PerformanceAnalysis")
        val RefactorTask = TaskType("RefactorTask")
        val ForeachTask = TaskType("ForeachTask")

        init {
            // Register built-in task types
            register(TaskPlanning) { settings, task -> PlanningTask(settings, task) }
            register(Inquiry) { settings, task -> InquiryTask(settings, task) }
            register(FileModification) { settings, task -> FileModificationTask(settings, task) }
            register(Documentation) { settings, task -> DocumentationTask(settings, task) }
            register(RunShellCommand) { settings, task -> RunShellCommandTask(settings, task) }
            register(CommandAutoFix) { settings, task -> CommandAutoFixTask(settings, task) }
            register(CodeReview) { settings, task -> CodeReviewTask(settings, task) }
            register(TestGeneration) { settings, task -> TestGenerationTask(settings, task) }
            register(Optimization) { settings, task -> CodeOptimizationTask(settings, task) }
            register(SecurityAudit) { settings, task -> SecurityAuditTask(settings, task) }
            register(PerformanceAnalysis) { settings, task -> PerformanceAnalysisTask(settings, task) }
            register(RefactorTask) { settings, task -> RefactorTask(settings, task) }
            register(ForeachTask) { settings, task -> ForeachTask(settings, task) }
        }

        fun register(taskType: TaskType, constructor: (PlanSettings, PlanningTask.PlanTask) -> AbstractTask) {
            registry[taskType.name] = taskType
            taskConstructors[taskType] = constructor
        }

        fun getTaskType(name: String): TaskType? {
            return registry[name]
        }

        fun values(): Collection<TaskType> = registry.values
        fun getImpl(planSettings: PlanSettings, planTask: PlanningTask.PlanTask): AbstractTask {
            val taskType = planTask.taskType ?: throw RuntimeException("Task type is null")
            if (!planSettings.getTaskSettings(taskType).enabled) {
                throw DisabledTaskException(taskType)
            }
            val constructor =
                taskConstructors[taskType] ?: throw RuntimeException("Unknown task type: ${taskType.name}")
            return constructor(planSettings, planTask)
        }

        fun getAvailableTaskTypes(planSettings: PlanSettings): List<AbstractTask> = TaskType.values().filter {
            planSettings.getTaskSettings(it).enabled
        }.map { getImpl(planSettings, PlanningTask.PlanTask(taskType = it)) }

        fun valueOf(name: String): TaskType {
            return registry[name] ?: throw IllegalArgumentException("No enum constant $name")
        }
    }
}