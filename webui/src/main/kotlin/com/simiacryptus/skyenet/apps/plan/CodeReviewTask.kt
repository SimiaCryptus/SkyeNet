package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.skyenet.apps.plan.CodeReviewTask.CodeReviewTaskData

import org.slf4j.LoggerFactory

class CodeReviewTask(
    planSettings: PlanSettings,
    planTask: CodeReviewTaskData?
) : AbstractAnalysisTask<CodeReviewTaskData>(planSettings, planTask) {
    class CodeReviewTaskData(
        @Description("List of files to be reviewed")
        val filesToReview: List<String>? = null,
        @Description("Specific areas of focus for the review (optional)")
        val focusAreas: List<String>? = null,
        task_type: String? = null,
        task_description: String? = null,
        task_dependencies: List<String>? = null,
        input_files: List<String>? = null,
        output_files: List<String>? = null,
        state: TaskState? = null
    ) : PlanTaskBase(
        task_type = task_type,
        task_description = task_description,
        task_dependencies = task_dependencies,
        input_files = input_files,
        output_files = output_files,
        state = state
    )

    override val actorName: String = "CodeReview"
    override val actorPrompt: String = """
 Perform a comprehensive code review for the provided code files. Analyze the code for:
 1. Code quality and readability
 2. Potential bugs or errors
 3. Performance issues
 4. Security vulnerabilities
 5. Adherence to best practices and coding standards
 6. Suggestions for improvements or optimizations
                |
 Provide a detailed review with specific examples and recommendations for each issue found.
 Format the response as a markdown document with appropriate headings and code snippets.
    """.trimIndent()

    override fun getAnalysisInstruction(): String {
        val filesToReview = planTask?.filesToReview?.joinToString(", ") ?: "all provided files"
        val focusAreas = planTask?.focusAreas?.joinToString(", ")
        return "Review the following code files: $filesToReview" +
                if (focusAreas != null) ". Focus on these areas: $focusAreas" else ""
    }

    override fun promptSegment(): String {
        return """
 CodeReview - Perform an automated code review and provide suggestions for improvements
   ** Specify the files to be reviewed
   ** Optionally provide specific areas of focus for the review
        """.trimMargin()
    }


    companion object {
        private val log = LoggerFactory.getLogger(CodeReviewTask::class.java)
    }
}