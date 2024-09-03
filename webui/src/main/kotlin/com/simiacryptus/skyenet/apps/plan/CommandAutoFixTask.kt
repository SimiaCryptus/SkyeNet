package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.skyenet.Retryable
import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.apps.general.CmdPatchApp
import com.simiacryptus.skyenet.apps.general.PatchApp
import com.simiacryptus.skyenet.core.actors.ParsedResponse
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.util.MarkdownUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.Semaphore

class CommandAutoFixTask(
    settings: Settings,
    planTask: PlanTask
) : AbstractTask(settings, planTask) {
    override fun promptSegment(): String {
        return """
            |CommandAutoFix - Run a command and automatically fix any issues that arise
            |  ** Specify the command to be executed and any additional instructions
            |  ** Specify the working directory relative to the root directory
            |  ** Provide the command arguments in the 'commandArguments' field
            |  ** List input files/tasks to be examined when fixing issues
            |  ** Available commands:
            |    ${settings.commandAutoFixCommands.joinToString("\n    ") { "* ${File(it).name}" }}
        """.trimMargin()
    }

    override fun run(
        agent: PlanCoordinator,
        taskId: String,
        userMessage: String,
        plan: PlanCoordinator.TaskBreakdownResult,
        genState: PlanCoordinator.GenState,
        task: SessionTask,
        taskTabs: TabbedDisplay
    ) {
        val semaphore = Semaphore(0)
        val onComplete = {
            semaphore.release()
        }
        if (!agent.settings.enableCommandAutoFix) {
            task.add("Command Auto Fix is disabled")
            onComplete()
        } else {
            Retryable(agent.ui, task = task) {
                val task = agent.ui.newTask(false).apply { it.append(placeholder) }
                val alias = this.planTask.command?.first()
                val commandAutoFixCommands = agent.settings.commandAutoFixCommands
                val cmds = commandAutoFixCommands.filter {
                    File(it).name.startsWith(alias ?: "")
                }
                val executable = cmds.firstOrNull()
                if (executable == null) {
                    throw IllegalArgumentException("Command not found: $alias")
                }
                val workingDirectory = (this.planTask.workingDir
                    ?.let { agent.root.toFile().resolve(it) } ?: agent.root.toFile())
                    .apply { mkdirs() }
                val outputResult = CmdPatchApp(
                    root = agent.root,
                    session = agent.session,
                    settings = PatchApp.Settings(
                        executable = File(executable),
                        arguments = this.planTask.command?.drop(1)?.joinToString(" ") ?: "",
                        workingDirectory = workingDirectory,
                        exitCodeOption = "nonzero",
                        additionalInstructions = "",
                        autoFix = agent.settings.autoFix
                    ),
                    api = agent.api as OpenAIClient,
                    files = agent.files,
                    model = agent.settings.model,
                ).run(
                    ui = agent.ui,
                    task = task
                )
                genState.taskResult[taskId] = "Command Auto Fix completed"
                task.add(if (outputResult.exitCode == 0) {
                    if (agent.settings.autoFix) {
                        taskTabs.selectedTab += 1
                        taskTabs.update()
                        onComplete()
                        MarkdownUtil.renderMarkdown("## Auto-applied Command Auto Fix\n", ui = agent.ui)
                    } else {
                        MarkdownUtil.renderMarkdown(
                            "## Command Auto Fix Result\n",
                            ui = agent.ui
                        ) + acceptButtonFooter(
                            agent.ui
                        ) {
                            taskTabs.selectedTab += 1
                            taskTabs.update()
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
                        taskTabs.selectedTab += 1
                        taskTabs.update()
                        onComplete()
                    }
                })
                task.placeholder
            }
        }
        try {
            semaphore.acquire()
        } catch (e: Throwable) {
            PlanCoordinator.log.warn("Error", e)
        }
        PlanCoordinator.log.debug("Completed command auto fix: $taskId")
    }

    companion object {
        private val log = LoggerFactory.getLogger(CommandAutoFixTask::class.java)
    }
}