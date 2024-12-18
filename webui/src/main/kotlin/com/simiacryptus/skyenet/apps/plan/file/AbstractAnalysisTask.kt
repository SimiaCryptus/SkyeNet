package com.simiacryptus.skyenet.apps.plan.file

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.skyenet.apps.general.CommandPatchApp
import com.simiacryptus.skyenet.apps.general.PatchApp
import com.simiacryptus.skyenet.apps.plan.*
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.webui.session.SessionTask
import org.slf4j.LoggerFactory
import java.io.File

abstract class AbstractAnalysisTask<T : AbstractFileTask.FileTaskConfigBase>(
  planSettings: PlanSettings,
  planTask: T?
) : AbstractFileTask<T>(planSettings, planTask) {

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
    messages: List<String>,
    task: SessionTask,
    api: ChatClient,
    resultFn: (String) -> Unit,
    api2: OpenAIClient,
    planSettings: PlanSettings
  ) {
    val analysisResult = analysisActor.answer(
      messages + listOf(
        getInputFileCode(),
        "${getAnalysisInstruction()}:\n${getInputFileCode()}",
      ).filter { it.isNotBlank() }, api = api
    )
    resultFn(analysisResult)
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