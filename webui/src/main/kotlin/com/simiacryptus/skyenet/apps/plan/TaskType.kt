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
                TaskPlanning -> if (planSettings.taskPlanningEnabled) PlanningTask(
                    planSettings,
                    planTask
                ) else throw DisabledTaskException(planTask.taskType)

                Documentation -> if (planSettings.documentationEnabled) DocumentationTask(
                    planSettings,
                    planTask
                ) else throw DisabledTaskException(planTask.taskType)

                FileModification -> if (planSettings.fileModificationEnabled) FileModificationTask(
                    planSettings,
                    planTask
                ) else throw DisabledTaskException(planTask.taskType)

                RunShellCommand -> if (planSettings.shellCommandTaskEnabled) RunShellCommandTask(
                    planSettings,
                    planTask
                ) else throw DisabledTaskException(planTask.taskType)

                CommandAutoFix -> if (planSettings.enableCommandAutoFix) CommandAutoFixTask(
                    planSettings,
                    planTask
                ) else throw DisabledTaskException(planTask.taskType)

                Inquiry -> if (planSettings.inquiryEnabled) InquiryTask(
                    planSettings,
                    planTask
                ) else throw DisabledTaskException(planTask.taskType)

                CodeReview -> if (planSettings.codeReviewEnabled) CodeReviewTask(
                    planSettings,
                    planTask
                ) else throw DisabledTaskException(planTask.taskType)

                TestGeneration -> if (planSettings.testGenerationEnabled) TestGenerationTask(
                    planSettings,
                    planTask
                ) else throw DisabledTaskException(planTask.taskType)

                Optimization -> if (planSettings.optimizationEnabled) CodeOptimizationTask(
                    planSettings,
                    planTask
                ) else throw DisabledTaskException(planTask.taskType)

                SecurityAudit -> if (planSettings.securityAuditEnabled) SecurityAuditTask(
                    planSettings,
                    planTask
                ) else throw DisabledTaskException(planTask.taskType)

                PerformanceAnalysis -> if (planSettings.performanceAnalysisEnabled) PerformanceAnalysisTask(
                    planSettings,
                    planTask
                ) else throw DisabledTaskException(planTask.taskType)

                RefactorTask -> if (planSettings.refactorTaskEnabled) RefactorTask(
                    planSettings,
                    planTask
                ) else throw DisabledTaskException(planTask.taskType)

                ForeachTask -> if (planSettings.foreachTaskEnabled) ForeachTask(
                    planSettings,
                    planTask
                ) else throw DisabledTaskException(planTask.taskType)

                else -> throw RuntimeException("Unknown task type: ${planTask.taskType}")
            }
        }

        fun getAvailableTaskTypes(planSettings: PlanSettings): List<AbstractTask> = TaskType.values().filter {
            when (it) {
                TaskPlanning -> planSettings.taskPlanningEnabled
                RunShellCommand -> planSettings.shellCommandTaskEnabled
                Documentation -> planSettings.documentationEnabled
                FileModification -> planSettings.fileModificationEnabled
                Inquiry -> planSettings.inquiryEnabled
                CodeReview -> planSettings.codeReviewEnabled
                TestGeneration -> planSettings.testGenerationEnabled
                Optimization -> planSettings.optimizationEnabled
                CommandAutoFix -> planSettings.enableCommandAutoFix
                SecurityAudit -> planSettings.securityAuditEnabled
                PerformanceAnalysis -> planSettings.performanceAnalysisEnabled
                RefactorTask -> planSettings.refactorTaskEnabled
                ForeachTask -> planSettings.foreachTaskEnabled
            }
        }.map { getImpl(planSettings, PlanningTask.PlanTask(taskType = it)) }
    }
}