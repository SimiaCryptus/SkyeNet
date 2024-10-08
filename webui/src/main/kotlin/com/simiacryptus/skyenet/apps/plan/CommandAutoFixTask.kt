package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.skyenet.Retryable
import com.simiacryptus.skyenet.apps.general.CmdPatchApp
import com.simiacryptus.skyenet.apps.general.PatchApp
import com.simiacryptus.skyenet.apps.plan.CommandAutoFixTask.CommandAutoFixTaskData
import com.simiacryptus.skyenet.util.MarkdownUtil
import com.simiacryptus.skyenet.webui.session.SessionTask
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.Semaphore

class CommandAutoFixTask(
    planSettings: PlanSettings,
    planTask: CommandAutoFixTaskData?
) : AbstractTask<CommandAutoFixTaskData>(planSettings, planTask) {
    class CommandAutoFixTaskData(
        @Description("The command to be executed")
        val command: List<String>? = null,
        @Description("The working directory for the command execution")
        val workingDir: String? = null,
        task_description: String? = null,
        task_dependencies: List<String>? = null,
        state: TaskState? = null
    ) : PlanTaskBase(
        task_type = TaskType.CommandAutoFix.name,
        task_description = task_description,
        task_dependencies = task_dependencies,
        state = state
    )

    override fun promptSegment(): String {
        return """
CommandAutoFix - Run a command and automatically fix any issues that arise
    ** Specify the command to be executed and any additional instructions
    ** Specify the working directory relative to the root directory
    ** Provide the command and its arguments in the 'command' field
    ** List input files/tasks to be examined when fixing issues
    ** Available commands:
    ${planSettings.commandAutoFixCommands?.joinToString("\n") { "    * ${File(it).name}" }}
        """.trimMargin()
    }

    override fun run(
        agent: PlanCoordinator,
        taskId: String,
        userMessage: String,
        plan: Map<String, PlanTaskBase>,
        planProcessingState: PlanProcessingState,
        task: SessionTask,
        api: API,
        resultFn: (String) -> Unit
    ) {
        val semaphore = Semaphore(0)
        val onComplete = {
            semaphore.release()
        }
        Retryable(agent.ui, task = task) {
            val task = agent.ui.newTask(false).apply { it.append(placeholder) }
            val alias = this.planTask?.command?.first()
            val commandAutoFixCommands = agent.planSettings.commandAutoFixCommands
            val cmds = commandAutoFixCommands
                ?.map { File(it) }?.associateBy { it.name }
                ?.filterKeys { it.startsWith(alias ?: "") }
                ?: emptyMap()
            val executable = cmds.entries.firstOrNull()?.value
            if (executable == null) {
                throw IllegalArgumentException("Command not found: $alias")
            }
            val workingDirectory = (this.planTask?.workingDir
                ?.let { agent.root.toFile().resolve(it) } ?: agent.root.toFile())
                .apply { mkdirs() }
            val outputResult = CmdPatchApp(
                root = agent.root,
                session = agent.session,
                settings = PatchApp.Settings(
                    executable = executable,
                    arguments = this.planTask?.command?.drop(1)?.joinToString(" ") ?: "",
                    workingDirectory = workingDirectory,
                    exitCodeOption = "nonzero",
                    additionalInstructions = "",
                    autoFix = agent.planSettings.autoFix
                ),
                api = api as ChatClient,
                files = agent.files,
                model = agent.planSettings.getTaskSettings(TaskType.valueOf(planTask?.task_type!!)).model
                    ?: agent.planSettings.defaultModel,
            ).run(
                ui = agent.ui,
                task = task
            )
            resultFn("Command Auto Fix completed")
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