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
import java.util.concurrent.atomic.AtomicBoolean

class CommandAutoFixTask(
    planSettings: PlanSettings,
    planTask: CommandAutoFixTaskData?
) : AbstractTask<CommandAutoFixTaskData>(planSettings, planTask) {

    class CommandAutoFixTaskData(
        @Description("The commands to be executed with their respective working directories")
        val commands: List<CommandWithWorkingDir>? = null,
        task_description: String? = null,
        task_dependencies: List<String>? = null,
        state: TaskState? = null
    ) : PlanTaskBase(
        task_type = TaskType.CommandAutoFix.name,
        task_description = task_description,
        task_dependencies = task_dependencies,
        state = state
    )

    data class CommandWithWorkingDir(
        @Description("The command to be executed")
        val command: List<String>,
        @Description("The working directory for this specific command")
        val workingDir: String? = null
    )

    override fun promptSegment(): String {
        return """
CommandAutoFix - Run a command and automatically fix any issues that arise
** Specify the commands to be executed along with their working directories
** Each command's working directory should be specified relative to the root directory
** Provide the commands and their arguments in the 'commands' field
** Each command should be a list of strings
** Available commands:
${planSettings.commandAutoFixCommands?.joinToString("\n") { "    * ${File(it).name}" }}
        """.trim()
    }

    override fun run(
        agent: PlanCoordinator,
        taskId: String,
        messages: List<String>,
        task: SessionTask,
        api: API,
        resultFn: (String) -> Unit
    ) {
        val semaphore = Semaphore(0)
        val hasError = AtomicBoolean(false)
        val onComplete = {
            semaphore.release()
        }
        Retryable(agent.ui, task = task) {
            val task = agent.ui.newTask(false).apply { it.append(placeholder) }
            this.planTask?.commands?.forEachIndexed { index, commandWithDir ->
                val alias = commandWithDir.command.firstOrNull()
                val commandAutoFixCommands = agent.planSettings.commandAutoFixCommands
                val cmds = commandAutoFixCommands
                    ?.map { File(it) }?.associateBy { it.name }
                    ?.filterKeys { it.startsWith(alias ?: "") }
                    ?: emptyMap()
                val executable = cmds.entries.firstOrNull()?.value
                if (executable == null) {
                    throw IllegalArgumentException("Command not found: $alias")
                }
                val workingDirectory = (commandWithDir.workingDir
                    ?.let { agent.root.toFile().resolve(it) } ?: agent.root.toFile())
                    .apply { mkdirs() }
                val outputResult = CmdPatchApp(
                    root = agent.root,
                    session = agent.session,
                    settings = PatchApp.Settings(
                        executable = executable,
                        arguments = commandWithDir.command.drop(1).joinToString(" "),
                        workingDirectory = workingDirectory,
                        exitCodeOption = "nonzero",
                        additionalInstructions = "",
                        autoFix = agent.planSettings.autoFix
                    ),
                    api = api as ChatClient,
                    files = agent.files,
                    model = agent.planSettings.getTaskSettings(TaskType.valueOf(planTask.task_type!!)).model
                        ?: agent.planSettings.defaultModel,
                ).run(
                    ui = agent.ui,
                    task = task
                )
                if (outputResult.exitCode != 0) {
                    hasError.set(true)
                }
                task.add(MarkdownUtil.renderMarkdown("## Command Auto Fix Result for Command ${index + 1}\n", ui = agent.ui, tabs = false))
                task.add(
                    if (outputResult.exitCode == 0) {
                        if (agent.planSettings.autoFix) {
                            MarkdownUtil.renderMarkdown("Auto-applied Command Auto Fix\n", ui = agent.ui, tabs = false)
                        } else {
                            MarkdownUtil.renderMarkdown(
                                "Command Auto Fix Result\n",
                                ui = agent.ui, tabs = false
                            )
                        }
                    } else {
                        MarkdownUtil.renderMarkdown(
                            "Command Auto Fix Failed\n",
                            ui = agent.ui, tabs = false
                        )
                    }
                )
            }
            resultFn("All Command Auto Fix tasks completed")
            task.add(if (!hasError.get()) {
                onComplete()
                MarkdownUtil.renderMarkdown("## All Command Auto Fix tasks completed successfully\n", ui = agent.ui, tabs = false)
            } else {
                MarkdownUtil.renderMarkdown(
                    "## Some Command Auto Fix tasks failed\n",
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