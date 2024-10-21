package com.simiacryptus.skyenet.apps.general

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.models.TextModel
import com.simiacryptus.skyenet.apps.plan.AbstractTask
import com.simiacryptus.skyenet.apps.plan.PlanCoordinator
import com.simiacryptus.skyenet.apps.plan.PlanProcessingState
import com.simiacryptus.skyenet.apps.plan.PlanSettings
import com.simiacryptus.skyenet.apps.plan.PlanTaskBase
import com.simiacryptus.skyenet.apps.plan.TaskType
import com.simiacryptus.skyenet.core.actors.ParsedActor
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.util.MarkdownUtil
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.util.JsonUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.util.UUID

open class AutoPlanChatApp(
    applicationName: String = "Auto Plan Chat App",
    path: String = "/autoPlanChat",
    planSettings: PlanSettings,
    model: TextModel,
    parsingModel: TextModel,
    domainName: String = "localhost",
    showMenubar: Boolean = true,
    api: API? = null,
) : PlanChatApp(
    applicationName = applicationName,
    path = path,
    planSettings = planSettings,
    model = model,
    parsingModel = parsingModel,
    domainName = domainName,
    showMenubar = showMenubar,
    api = api,
) {
    companion object {
        private val log = LoggerFactory.getLogger(AutoPlanChatApp::class.java)
    }

    data class ThinkingStatus(
        var initialPrompt: String = "",
        val openQuestions: MutableList<String> = mutableListOf(),
        val completedTasks: MutableList<String> = mutableListOf(),
    )

    private var thinkingStatus : ThinkingStatus? = null
    private lateinit var thinkingStatusActor: ParsedActor<ThinkingStatus>
    private lateinit var updateQuestionsActor: ParsedActor<ThinkingStatus>
    private var currentUserMessage : String? = null

    override fun userMessage(session: Session, user: User?, userMessage: String, ui: ApplicationInterface, api: API) {
        try {
            log.info("Received user message: $userMessage")
            if (thinkingStatus == null) {
                log.info("Starting new auto plan chat")
                startAutoPlanChat(session, user, userMessage, ui, api)
            } else {
                log.info("Injecting user message into ongoing chat")
                injectUserMessage(userMessage, ui)
            }
        } catch (e: Exception) {
            log.error("Error processing user message", e)
            ui.newTask().add(MarkdownUtil.renderMarkdown("An error occurred while processing your message: ${e.message}", ui = ui))
        }
    }

    private fun startAutoPlanChat(session: Session, user: User?, userMessage: String, ui: ApplicationInterface, api: API) {
        val task = ui.newTask()
        val api = (api as ChatClient).getChildClient().apply {
            val createFile = task.createFile(".logs/api-${UUID.randomUUID()}.log")
            createFile.second?.apply {
                logStreams += this.outputStream().buffered()
                task.verbose("API log: <a href=\"file:///$this\">$this</a>")
            }
        }
        ui.socketManager!!.pool.execute {
            try {
                task.add("Starting Auto Plan Chat for prompt: $userMessage")
                val planSettings = getSettings(session, user, PlanSettings::class.java) ?: planSettings.copy(allowBlocking = false)
                api.budget = planSettings.budget
                val coordinator = PlanCoordinator(
                    user = user,
                    session = session,
                    dataStorage = dataStorage,
                    ui = ui,
                    root = planSettings.workingDir?.let { File(it).toPath() } ?: dataStorage.getDataDir(user, session).toPath(),
                    planSettings = planSettings
                )
                initializeThinkingStatusActor(planSettings)
                initializeUpdateQuestionsActor(planSettings)
                initializeThinkingStatus(userMessage)
                task.add(MarkdownUtil.renderMarkdown("Initial thinking status:\n${formatThinkingStatus(thinkingStatus!!)}", ui = ui))

                while (true) {
                    val nextTask = try {
                        chooseNextTask(coordinator, userMessage, thinkingStatus!!, api)?.planTask
                    } catch (e: Exception) {
                        log.error("Error choosing next task", e)
                        task.add(MarkdownUtil.renderMarkdown("Error choosing next task: ${e.message}", ui = ui))
                        break
                    }
                    if (nextTask == null) {
                        task.add(MarkdownUtil.renderMarkdown("No more tasks to execute. Finishing Auto Plan Chat.", ui = ui))
                        break
                    }
                    val currentTaskId = "task_${thinkingStatus!!.completedTasks.size + 1}"
                    val currentTask = nextTask
                    task.add(MarkdownUtil.renderMarkdown("Executing task: `$currentTaskId` - ${currentTask.task_description}", ui = ui))

                    val taskResult = try {
                        executeTask(coordinator, currentTaskId, currentTask, userMessage, api)
                    } catch (e: Exception) {
                        log.error("Error executing task", e)
                        "Error executing task: ${e.message}"
                    }

                    task.add(MarkdownUtil.renderMarkdown("Updated thinking status:\n${formatThinkingStatus(thinkingStatus!!)}", ui = ui))
                    task.add(MarkdownUtil.renderMarkdown("Task result:\n```\n$taskResult\n```", ui = ui))

                    ui.newTask().add(MarkdownUtil.renderMarkdown("User update: Completed task `$currentTaskId` - ${currentTask.task_description}", ui = ui))
                    thinkingStatus = updateOpenQuestions(thinkingStatus!!, currentTask, taskResult, api)
                }
            } catch (e: Exception) {
                log.error("Error in startAutoPlanChat", e)
                task.add(MarkdownUtil.renderMarkdown("An error occurred during the Auto Plan Chat: ${e.message}", ui = ui))
            } finally {
                task.add(MarkdownUtil.renderMarkdown("Auto Plan Chat completed. Final thinking status:\n${thinkingStatus?.let {
                    formatThinkingStatus(it)
                } ?: "null"}", ui = ui))
            }
        }

    }

    private fun initializeThinkingStatusActor(planSettings: PlanSettings) {
        thinkingStatusActor = ParsedActor(
            name = "ThinkingStatusInitializer",
            resultClass = ThinkingStatus::class.java,
            exampleInstance = ThinkingStatus(
                initialPrompt = "Example prompt",
                openQuestions = mutableListOf("What is the first task?"),
                completedTasks = mutableListOf(),
            ),
            prompt = """
                Given the user's initial prompt, initialize the thinking status for an AI assistant.
                Generate relevant open questions to guide the planning process.
            """.trimIndent(),
            model = planSettings.defaultModel,
            parsingModel = planSettings.parsingModel,
            temperature = planSettings.temperature,
            describer = planSettings.describer()
        )
    }

    private fun initializeUpdateQuestionsActor(planSettings: PlanSettings) {
        updateQuestionsActor = ParsedActor(
            name = "UpdateQuestionsActor",
            resultClass = ThinkingStatus::class.java,
            exampleInstance = ThinkingStatus(
                initialPrompt = "Example prompt",
                openQuestions = mutableListOf("What is the next task?", "Are there any remaining tasks?"),
                completedTasks = mutableListOf("task_1"),
            ),
            prompt = """
                Given the current thinking status, the last completed task, and its result,
                update the open questions to guide the next steps of the planning process.
                Consider what information is still needed and what new questions arise from the task result.
            """.trimIndent(),
            model = planSettings.defaultModel,
            parsingModel = planSettings.parsingModel,
            temperature = planSettings.temperature,
            describer = planSettings.describer()
        )
    }

    private fun initializeThinkingStatus(userMessage: String) {
        thinkingStatus = thinkingStatusActor.answer(initialPrompt(userMessage), api!!).obj
        thinkingStatus?.initialPrompt = userMessage
    }

    private fun injectUserMessage(userMessage: String, ui: ApplicationInterface) {
        val task = ui.newTask()
        task.echo(MarkdownUtil.renderMarkdown(userMessage, ui = ui))
        currentUserMessage = userMessage
    }

    private fun updateOpenQuestions(thinkingStatus: ThinkingStatus, currentTask: PlanTaskBase, taskResult: String, api: API): ThinkingStatus {
        val updatedStatus = updateQuestionsActor.answer(
            initialPrompt("Current thinking status: ${formatThinkingStatus(thinkingStatus)}") +
            listOf(
                "Last completed task: ${currentTask.task_description}",
                "Task result: $taskResult"
            ) + (currentUserMessage?.let { listOf("User message: $it") } ?: listOf()),
            api
        ).obj
        currentUserMessage = null
        return updatedStatus
    }

    private fun formatThinkingStatus(thinkingStatus: ThinkingStatus) = """```json
${JsonUtil.toJson(thinkingStatus)}
```"""

    private fun chooseNextTask(
        coordinator: PlanCoordinator,
        userMessage: String,
        thinkingStatus: ThinkingStatus,
        api: API
    ): AbstractTask<out PlanTaskBase>? {
        val chooserResult = coordinator.planSettings.chooseSingleTask(
            listOf(
                """
                    Original user message: $userMessage
                    Current thinking status:
                    ${formatThinkingStatus(thinkingStatus)}
                    Please choose the next single task to execute based on the current status.
                    If there are no tasks to execute, return {}.
                """.trimIndent()
            ), api).obj
        return if (chooserResult.task_type == null) {
            null
        } else {
            TaskType.Companion.getImpl(coordinator.planSettings, chooserResult)
        }
    }

    protected open fun initialPrompt(userMessage: String): List<String> = listOf(userMessage)

    private fun executeTask(
        coordinator: PlanCoordinator,
        taskId: String,
        task: PlanTaskBase?,
        userMessage: String,
        api: API
    ): String {
        val taskImpl = TaskType.Companion.getImpl(coordinator.planSettings, task)
        val result = StringBuilder()
        taskImpl.run(
            agent = coordinator,
            taskId = taskId,
            userMessage = userMessage,
            plan = mapOf(taskId to task!!),
            planProcessingState = PlanProcessingState(mutableMapOf(taskId to task)),
            task = coordinator.ui.newTask(),
            api = api,
            resultFn = { result.append(it) }
        )
        return result.toString()
    }
}