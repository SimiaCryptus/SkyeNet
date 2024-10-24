package com.simiacryptus.skyenet.apps.plan

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.models.ApiModel
import com.simiacryptus.skyenet.Discussable
import com.simiacryptus.skyenet.core.actors.ParsedResponse
import com.simiacryptus.skyenet.util.MarkdownUtil
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.session.SessionTask
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.util.UUID

open class Planner {

    open fun initialPlan(
        codeFiles: Map<Path, String>,
        files: Array<File>,
        root: Path,
        task: SessionTask,
        userMessage: String,
        ui: ApplicationInterface,
        planSettings: PlanSettings,
        api: API
    ): TaskBreakdownWithPrompt {
        val api = (api as ChatClient).getChildClient().apply {
            val createFile = task.createFile(".logs/api-${UUID.randomUUID()}.log")
            createFile.second?.apply {
                logStreams += this.outputStream().buffered()
                task.verbose("API log: <a href=\"file:///$this\">$this</a>")
            }
        }
        val toInput = inputFn(codeFiles, files, root)
        return if (planSettings.allowBlocking)
            Discussable(
                task = task,
                heading = MarkdownUtil.renderMarkdown(userMessage, ui = ui),
                userMessage = { userMessage },
                initialResponse = {
                    newPlan(
                        api,
                        planSettings,
                        toInput(userMessage)
                    )
                },
                outputFn = {
                    try {
                        PlanUtil.render(
                            withPrompt = TaskBreakdownWithPrompt(
                                prompt = userMessage,
                                plan = it.obj,
                                planText = it.text
                            ),
                            ui = ui
                        )
                    } catch (e: Throwable) {
                        log.warn("Error rendering task breakdown", e)
                        task.error(ui, e)
                        e.message ?: e.javaClass.simpleName
                    }
                },
                ui = ui,
                reviseResponse = { userMessages: List<Pair<String, ApiModel.Role>> ->
                    newPlan(
                        api,
                        planSettings,
                        userMessages.map { it.first })
                },
            ).call().let {
                TaskBreakdownWithPrompt(
                    prompt = userMessage,
                    plan = PlanUtil.filterPlan { it.obj } ?: emptyMap(),
                    planText = it.text
                )
            }
        else newPlan(
            api,
            planSettings,
            toInput(userMessage)
        ).let {
            TaskBreakdownWithPrompt(
                prompt = userMessage,
                plan = PlanUtil.filterPlan { it.obj } ?: emptyMap(),
                planText = it.text
            )
        }
    }

    open fun newPlan(
        api: API,
        planSettings: PlanSettings,
        inStrings: List<String>
    ): ParsedResponse<Map<String, PlanTaskBase>> {
        val planningActor = planSettings.planningActor()
        return planningActor.respond(
            messages = planningActor.chatMessages(inStrings),
            input = inStrings,
            api = api
        ).map(Map::class.java) { it.tasksByID ?: emptyMap<String, PlanTaskBase>() } as ParsedResponse<Map<String, PlanTaskBase>>
    }


    open fun inputFn(
        codeFiles: Map<Path, String>,
        files: Array<File>,
        root: Path
    ) = { str: String ->
        listOf(
            if (!codeFiles.all { it.key.toFile().isFile } || codeFiles.size > 2) """
                                        |Files:
                                        |${codeFiles.keys.joinToString("\n") { "* $it" }}  
                                         """.trimMargin() else {
                files.joinToString("\n\n") {
                    val path = root.relativize(it.toPath())
                    """
                        |## $path
                        |
                        |${(codeFiles[path] ?: "").let { "$TRIPLE_TILDE\n${it/*.indent("  ")*/}\n$TRIPLE_TILDE" }}
                        """.trimMargin()
                }
            },
            str
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(Planner::class.java)
    }
}