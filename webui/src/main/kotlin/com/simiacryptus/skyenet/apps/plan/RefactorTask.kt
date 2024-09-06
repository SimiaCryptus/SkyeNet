package com.simiacryptus.skyenet.apps.plan

import org.slf4j.LoggerFactory

class RefactorTask(
    planSettings: PlanSettings,
    planTask: PlanningTask.PlanTask
) : AbstractAnalysisTask(planSettings, planTask) {
    override val actorName: String = "Refactor"
    override val actorPrompt: String = """
Analyze the provided code and suggest refactoring to improve code structure, readability, and maintainability. Focus on:
1. Improving code organization
2. Reducing code duplication
3. Enhancing modularity
4. Applying design patterns where appropriate
5. Improving naming conventions
6. Simplifying complex logic

Provide detailed explanations for each suggested refactoring, including:
- The reason for the refactoring
- The expected benefits
- Any potential trade-offs or considerations

Format the response as a markdown document with appropriate headings and code snippets.
Use diff format to show the proposed changes clearly.
    """.trimIndent()

    override fun getAnalysisInstruction(): String = "Refactor the following code"

    override fun promptSegment(): String {
        return """
RefactorTask - Analyze and refactor existing code to improve structure, readability, and maintainability
  ** Specify the files to be refactored
  ** Optionally provide specific areas of focus for the refactoring (e.g., modularity, design patterns, naming conventions)
        """.trimMargin()
    }


    companion object {
        private val log = LoggerFactory.getLogger(RefactorTask::class.java)
    }
}