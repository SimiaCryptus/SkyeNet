package com.simiacryptus.skyenet.apps.plan

import org.slf4j.LoggerFactory

class CodeReviewTask(
    settings: Settings,
    planTask: PlanTask
) : AbstractAnalysisTask(settings, planTask) {
    override val actorName: String = "CodeReview"
    override val actorPrompt: String = """
 Perform a comprehensive code review for the provided code files. Analyze the code for:
 1. Code quality and readability
 2. Potential bugs or errors
                |3. Performance issues
                |4. Security vulnerabilities
                |5. Adherence to best practices and coding standards
                |6. Suggestions for improvements or optimizations
                |
 Provide a detailed review with specific examples and recommendations for each issue found.
 Format the response as a markdown document with appropriate headings and code snippets.
    """.trimIndent()

    override fun getAnalysisInstruction(): String = "Review the following code"

    override fun promptSegment(): String {
        return """
            |CodeReview - Perform an automated code review and provide suggestions for improvements
            |  ** Specify the files to be reviewed
            |  ** Optionally provide specific areas of focus for the review
        """.trimMargin()
    }


    companion object {
        private val log = LoggerFactory.getLogger(CodeReviewTask::class.java)
    }
}