package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.skyenet.apps.general.CommandPatchApp
import com.simiacryptus.skyenet.apps.general.PatchApp
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.util.JsonUtil
import org.slf4j.LoggerFactory
import java.io.File

abstract class AbstractAnalysisTask<T : PlanTaskBase>(
    planSettings: PlanSettings,
    planTask: T?
) : AbstractTask<T>(planSettings, planTask) {

    abstract val actorName: String
    abstract val actorPrompt: String

    protected val analysisActor by lazy {
        SimpleActor(
            name = actorName,
            prompt = actorPrompt,
            model = planSettings.getTaskSettings(TaskType.valueOf(planTask?.task_type!!)).model
                ?: planSettings.defaultModel,
            temperature = planSettings.temperature,
        )
    }

    override fun run(
        agent: PlanCoordinator,
        taskId: String,
        userMessage: String,
        plan: Map<String, PlanTaskBase>,
        planProcessingState: PlanProcessingState,
        task: SessionTask,
        api: API
    ) {
        val analysisResult = analysisActor.answer(
            listOf(
                userMessage,
                JsonUtil.toJson(plan),
                getPriorCode(planProcessingState),
                getInputFileCode(),
                "${getAnalysisInstruction()}:\n${getInputFileCode()}",
            ).filter { it.isNotBlank() }, api = api
        )
        planProcessingState.taskResult[taskId] = analysisResult
        applyChanges(agent, task, analysisResult, api)
    }

    abstract fun getAnalysisInstruction(): String

    private fun applyChanges(agent: PlanCoordinator, task: SessionTask, analysisResult: String, api: API) {
        val outputResult = CommandPatchApp(
            root = agent.root.toFile(),
            session = agent.session,
            settings = PatchApp.Settings(
                executable = File("dummy"),
                workingDirectory = agent.root.toFile(),
                exitCodeOption = "nonzero",
                additionalInstructions = "",
                autoFix = agent.planSettings.autoFix
            ),
            api = api as ChatClient,
            model = agent.planSettings.getTaskSettings(TaskType.valueOf(planTask?.task_type!!)).model
                ?: agent.planSettings.defaultModel,
            files = agent.files,
            command = analysisResult
        ).run(
            ui = agent.ui,
            task = task
        )
        if (outputResult.exitCode == 0) {
            task.add("${actorName} completed and suggestions have been applied successfully.")
        } else {
            task.add("${actorName} completed, but failed to apply suggestions. Exit code: ${outputResult.exitCode}")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(AbstractAnalysisTask::class.java)
    }
}