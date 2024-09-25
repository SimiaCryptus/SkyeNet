package com.simiacryptus.skyenet.apps.plan.file

import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.skyenet.apps.plan.PlanSettings
import com.simiacryptus.skyenet.apps.plan.TaskType
import com.simiacryptus.skyenet.apps.plan.file.PerformanceAnalysisTask.PerformanceAnalysisTaskData
import org.slf4j.LoggerFactory

class PerformanceAnalysisTask(
    planSettings: PlanSettings,
    planTask: PerformanceAnalysisTaskData?
) : AbstractAnalysisTask<PerformanceAnalysisTaskData>(planSettings, planTask) {
    class PerformanceAnalysisTaskData(
        @Description("Files to be analyzed for performance issues")
        val files_to_analyze: List<String>? = null,
        @Description("Specific areas of focus for the analysis (e.g., time complexity, memory usage, I/O operations)")
        val analysis_focus: List<String>? = null,
        task_description: String? = null,
        task_dependencies: List<String>? = null,
        input_files: List<String>? = null,
        output_files: List<String>? = null,
        state: TaskState? = null,
    ) : FileTaskBase(
        task_type = TaskType.PerformanceAnalysis.name,
        task_description = task_description,
        task_dependencies = task_dependencies,
        input_files = input_files,
        output_files = output_files,
        state = state
    )

    override val actorName = "PerformanceAnalysis"
    override val actorPrompt = """
Analyze the provided code for performance issues and bottlenecks. Focus exclusively on:
1. Time complexity of algorithms
2. Memory usage and potential leaks
3. I/O operations and network calls
4. Concurrency and parallelism opportunities
5. Caching and memoization possibilities

Provide detailed explanations for each identified performance issue, including:
 The reason it's a performance concern
 The potential impact on system performance
- Quantitative estimates of performance impact where possible

Format the response as a markdown document with appropriate headings and code snippets.
Do not provide code changes, focus on analysis and recommendations.
    """.trimIndent()

    override fun promptSegment(): String {
        return """
PerformanceAnalysis - Analyze code for performance issues and suggest improvements
  ** Specify the files to be analyzed
  ** Optionally provide specific areas of focus for the analysis (e.g., time complexity, memory usage, I/O operations)
        """.trimMargin()
    }
    fun getFiles(): List<String> {
        return planTask?.files_to_analyze ?: emptyList()
    }


    override fun getAnalysisInstruction(): String {
        return "Analyze the following code for performance issues and provide a detailed report"
    }

    companion object {
        private val log = LoggerFactory.getLogger(PerformanceAnalysisTask::class.java)
    }
}