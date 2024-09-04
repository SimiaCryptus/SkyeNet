package com.simiacryptus.skyenet.apps.plan

import org.slf4j.LoggerFactory


class PerformanceAnalysisTask(
    planSettings: PlanSettings,
    planTask: PlanningTask.PlanTask
) : AbstractAnalysisTask(planSettings, planTask) {
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

    override fun getAnalysisInstruction(): String {
        return "Analyze the following code for performance issues and provide a detailed report"
    }

    companion object {
        private val log = LoggerFactory.getLogger(PerformanceAnalysisTask::class.java)
    }
}