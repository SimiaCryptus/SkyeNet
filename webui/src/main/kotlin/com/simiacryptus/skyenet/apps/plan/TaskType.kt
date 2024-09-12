package com.simiacryptus.skyenet.apps.plan

enum class TaskType {
    TaskPlanning,
    Inquiry,
    FileModification,
    Documentation,
    RunShellCommand,
    CommandAutoFix,
    CodeReview,
    TestGeneration,
    Optimization,
    SecurityAudit,
    PerformanceAnalysis,
    RefactorTask,
    ForeachTask;

    companion object {

        fun getImpl(planSettings: PlanSettings, planTask: PlanningTask.PlanTask): AbstractTask {
            return when (planTask.taskType) {
                TaskPlanning -> if (planSettings.getTaskSettings(TaskPlanning).enabled) PlanningTask(
                    planSettings,
                    planTask
                ) else throw DisabledTaskException(planTask.taskType)

                Documentation -> if (planSettings.getTaskSettings(Documentation).enabled) DocumentationTask(
                    planSettings,
                    planTask
                ) else throw DisabledTaskException(planTask.taskType)

                FileModification -> if (planSettings.getTaskSettings(FileModification).enabled) FileModificationTask(
                    planSettings,
                    planTask
                ) else throw DisabledTaskException(planTask.taskType)

                RunShellCommand -> if (planSettings.getTaskSettings(RunShellCommand).enabled) RunShellCommandTask(
                    planSettings,
                    planTask
                ) else throw DisabledTaskException(planTask.taskType)

                CommandAutoFix -> if (planSettings.getTaskSettings(CommandAutoFix).enabled) CommandAutoFixTask(
                    planSettings,
                    planTask
                ) else throw DisabledTaskException(planTask.taskType)

                Inquiry -> if (planSettings.getTaskSettings(Inquiry).enabled) InquiryTask(
                    planSettings,
                    planTask
                ) else throw DisabledTaskException(planTask.taskType)

                CodeReview -> if (planSettings.getTaskSettings(CodeReview).enabled) CodeReviewTask(
                    planSettings,
                    planTask
                ) else throw DisabledTaskException(planTask.taskType)

                TestGeneration -> if (planSettings.getTaskSettings(TestGeneration).enabled) TestGenerationTask(
                    planSettings,
                    planTask
                ) else throw DisabledTaskException(planTask.taskType)

                Optimization -> if (planSettings.getTaskSettings(Optimization).enabled) CodeOptimizationTask(
                    planSettings,
                    planTask
                ) else throw DisabledTaskException(planTask.taskType)

                SecurityAudit -> if (planSettings.getTaskSettings(SecurityAudit).enabled) SecurityAuditTask(
                    planSettings,
                    planTask
                ) else throw DisabledTaskException(planTask.taskType)

                PerformanceAnalysis -> if (planSettings.getTaskSettings(PerformanceAnalysis).enabled) PerformanceAnalysisTask(
                    planSettings,
                    planTask
                ) else throw DisabledTaskException(planTask.taskType)

                RefactorTask -> if (planSettings.getTaskSettings(RefactorTask).enabled) RefactorTask(
                    planSettings,
                    planTask
                ) else throw DisabledTaskException(planTask.taskType)

                ForeachTask -> if (planSettings.getTaskSettings(ForeachTask).enabled) ForeachTask(
                    planSettings,
                    planTask
                ) else throw DisabledTaskException(planTask.taskType)

                else -> throw RuntimeException("Unknown task type: ${planTask.taskType}")
            }
        }

        fun getAvailableTaskTypes(planSettings: PlanSettings): List<AbstractTask> = TaskType.values().filter {
            planSettings.getTaskSettings(it).enabled
        }.map { getImpl(planSettings, PlanningTask.PlanTask(taskType = it)) }
    }
}