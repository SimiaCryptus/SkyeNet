package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.core.actors.ParsedResponse
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.apps.general.PatchApp
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.skyenet.apps.general.CommandPatchApp
import org.slf4j.LoggerFactory
import java.io.File

class CodeReviewTask(
    settings: Settings,
    planTask: PlanTask
) : AbstractTask(settings, planTask) {
    val codeReviewActor by lazy {
        SimpleActor(
            name = "CodeReview",
            prompt = """
                |Perform a comprehensive code review for the provided code files. Analyze the code for:
                |1. Code quality and readability
                |2. Potential bugs or errors
                |3. Performance issues
                |4. Security vulnerabilities
                |5. Adherence to best practices and coding standards
                |6. Suggestions for improvements or optimizations
                |
                |Provide a detailed review with specific examples and recommendations for each issue found.
                |Format the response as a markdown document with appropriate headings and code snippets.
            """.trimMargin(),
            model = settings.model,
            temperature = settings.temperature,
        )
    }

    override fun promptSegment(): String {
        return """
            |CodeReview - Perform an automated code review and provide suggestions for improvements
            |  ** Specify the files to be reviewed
            |  ** Optionally provide specific areas of focus for the review
        """.trimMargin()
    }

    override fun run(
        agent: PlanCoordinator,
        taskId: String,
        userMessage: String,
        plan: ParsedResponse<PlanCoordinator.TaskBreakdownResult>,
        genState: PlanCoordinator.GenState,
        task: SessionTask,
        taskTabs: TabbedDisplay
    ) {
        "Review the following code:\n${getInputFileCode()}"
        val reviewResult = codeReviewActor.answer(
            listOf(
                userMessage,
                plan.text,
                getPriorCode(genState),
                getInputFileCode(),
                "Review the following code:\n${getInputFileCode()}",
            ).filter { it.isNotBlank() }, api = agent.api
        )
        genState.taskResult[taskId] = reviewResult
        // Apply changes using CommandPatchApp
        val outputResult = CommandPatchApp(
            root = agent.root.toFile(),
            session = agent.session,
            settings = PatchApp.Settings(
                executable = File("dummy"),
                workingDirectory = agent.root.toFile(),
                exitCodeOption = "nonzero",
                additionalInstructions = "",
                autoFix = agent.settings.autoFix
            ),
            api = agent.api as OpenAIClient,
            model = agent.settings.model,
            files = agent.files,
            command = reviewResult
        ).run(
            ui = agent.ui,
            task = task
        )
        if (outputResult.exitCode == 0) {
            task.add("Changes from code review have been applied successfully.")
        } else {
            task.add("Failed to apply changes from code review. Exit code: ${outputResult.exitCode}")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(CodeReviewTask::class.java)
    }
}