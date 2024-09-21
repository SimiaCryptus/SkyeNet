package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.skyenet.Retryable
import com.simiacryptus.skyenet.apps.general.CmdPatchApp
import com.simiacryptus.skyenet.apps.general.PatchApp
import com.simiacryptus.skyenet.apps.plan.PlanningTask.PlanTask
import com.simiacryptus.skyenet.apps.plan.RunShellCommandTask.ExecutionTaskInterface
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.util.MarkdownUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.Semaphore

class CommandAutoFixTask(
    planSettings: PlanSettings,
    planTask: ExecutionTaskInterface?
) : AbstractTask<ExecutionTaskInterface>(planSettings, planTask) {
    override fun promptSegment(): String {
        return """
             |CommandAutoFix - Run a command and automatically fix any issues that arise
             |  ** Specify the command to be executed and any additional instructions
             |  ** Specify the working directory relative to the root directory
             |  ** Provide the command arguments in the 'commandArguments' field
             |  ** List input files/tasks to be examined when fixing issues
             |  ** Available commands:
             |${planSettings.commandAutoFixCommands?.joinToString("\n") { "    * ${File(it).name}" }}
        """.trimMargin()
    }

    override fun run(
        agent: PlanCoordinator,
        taskId: String,
        userMessage: String,
        plan: Map<String, PlanTaskBaseInterface>,
        planProcessingState: PlanProcessingState,
        task: SessionTask,
        api: API
    ) {
        val semaphore = Semaphore(0)
        val onComplete = {
            semaphore.release()
        }
        Retryable(agent.ui, task = task) {
            val task = agent.ui.newTask(false).apply { it.append(placeholder) }
            val alias = this.planTask?.execution_task?.command?.first()
            val commandAutoFixCommands = agent.planSettings.commandAutoFixCommands
            val cmds = commandAutoFixCommands
                ?.map { File(it) }?.associateBy { it.name }
                ?.filterKeys { it.startsWith(alias ?: "") }
                ?: emptyMap()
            val executable = cmds.entries.firstOrNull()?.value
            if (executable == null) {
                throw IllegalArgumentException("Command not found: $alias")
            }
            val workingDirectory = (this.planTask?.execution_task?.workingDir
                ?.let { agent.root.toFile().resolve(it) } ?: agent.root.toFile())
                .apply { mkdirs() }
            val outputResult = CmdPatchApp(
                root = agent.root,
                session = agent.session,
                settings = PatchApp.Settings(
                    executable = executable,
                    arguments = this.planTask?.execution_task?.command?.drop(1)?.joinToString(" ") ?: "",
                    workingDirectory = workingDirectory,
                    exitCodeOption = "nonzero",
                    additionalInstructions = "",
                    autoFix = agent.planSettings.autoFix
                ),
                api = api as ChatClient,
                files = agent.files,
                model = agent.planSettings.getTaskSettings(planTask?.task_type!! as TaskType<*>).model
                    ?: agent.planSettings.defaultModel,
            ).run(
                ui = agent.ui,
                task = task
            )
            planProcessingState.taskResult[taskId] = "Command Auto Fix completed"
            task.add(if (outputResult.exitCode == 0) {
                if (agent.planSettings.autoFix) {
                    onComplete()
                    MarkdownUtil.renderMarkdown("## Auto-applied Command Auto Fix\n", ui = agent.ui)
                } else {
                    MarkdownUtil.renderMarkdown(
                        "## Command Auto Fix Result\n",
                        ui = agent.ui
                    ) + acceptButtonFooter(
                        agent.ui
                    ) {
                        onComplete()
                    }
                }
            } else {
                MarkdownUtil.renderMarkdown(
                    "## Command Auto Fix Failed\n",
                    ui = agent.ui
                ) + acceptButtonFooter(
                    agent.ui
                ) {
                    onComplete()
                }
            })
            task.placeholder
        }
        try {
            semaphore.acquire()
        } catch (e: Throwable) {
            log.warn("Error", e)
        }
        log.debug("Completed command auto fix: $taskId")
    }

    companion object {
        private val log = LoggerFactory.getLogger(CommandAutoFixTask::class.java)
    }
}