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

class CodeOptimizationTask(
    settings: Settings,
    planTask: PlanTask
) : AbstractTask(settings, planTask) {
    val codeOptimizationActor by lazy {
        SimpleActor(
            name = "CodeOptimization",
            prompt = """
                |Analyze the provided code and suggest optimizations to improve performance, readability, and maintainability. Focus on:
                |1. Algorithmic improvements
                |2. Code structure and organization
                |3. Memory usage optimization
                |4. Time complexity reduction
                |5. Proper use of language-specific features and best practices
                |                
                |Provide detailed explanations for each suggested optimization, including:
                |- The reason for the optimization
                |- The expected benefits
                |- Any potential trade-offs or considerations
                |
                |Format the response as a markdown document with appropriate headings and code snippets.
                |Use diff format to show the proposed changes clearly.
            """.trimMargin(),
            model = settings.model,
            temperature = settings.temperature,
        )
    }

    override fun promptSegment(): String {
        return """
            |CodeOptimization - Analyze and optimize existing code for better performance, readability, and maintainability
            |  ** Specify the files to be optimized
            |  ** Optionally provide specific areas of focus for the optimization (e.g., performance, memory usage, readability)
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
        val optimizationResult = codeOptimizationActor.answer(
            listOf(
                userMessage,
                plan.text,
                getPriorCode(genState),
                getInputFileCode(),
                "Optimize the following code:\n${getInputFileCode()}",
            ).filter { it.isNotBlank() }, api = agent.api
        )
        genState.taskResult[taskId] = optimizationResult
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
            command = optimizationResult
        ).run(
            ui = agent.ui,
            task = task
        )
        if (outputResult.exitCode == 0) {
            task.add("Changes from code optimization have been applied successfully.")
        } else {
            task.add("Failed to apply changes from code optimization. Exit code: ${outputResult.exitCode}")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(CodeOptimizationTask::class.java)
    }
}