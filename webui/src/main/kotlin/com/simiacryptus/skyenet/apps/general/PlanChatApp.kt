package com.simiacryptus.skyenet.apps.general

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.models.OpenAITextModel
import com.simiacryptus.skyenet.apps.plan.*
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.util.MarkdownUtil
import org.slf4j.LoggerFactory
import java.io.File

class PlanChatApp(
    applicationName: String = "Task Planning Chat v1.0",
    path: String = "/taskChat",
    rootFile: File,
    planSettings: PlanSettings,
    model: OpenAITextModel,
    parsingModel: OpenAITextModel,
    domainName: String = "localhost",
    showMenubar: Boolean = true,
    initialPlan: PlanUtil.TaskBreakdownWithPrompt? = null,
    api: API? = null,
) : PlanAheadApp(
    applicationName = applicationName,
    path = path,
    rootFile = rootFile,
    planSettings = planSettings,
    model = model,
    parsingModel = parsingModel,
    domainName = domainName,
    showMenubar = showMenubar,
    initialPlan = initialPlan,
    api = api,
) {
    override val stickyInput = true
    override val singleInput = false

    private val sessionHandlers: MutableMap<String, ChatSessionHandler> = mutableMapOf()
    override fun userMessage(
        session: Session,
        user: User?,
        userMessage: String,
        ui: ApplicationInterface,
        api: API
    ) {
        ui.socketManager?.pool!!.submit {
            val sessionHandler = sessionHandlers.getOrPut(session.sessionId) {
                ChatSessionHandler(
                    ui = ui,
                    session = session,
                    user = user,
                    api = api
                )
            }
            sessionHandler.handleUserMessage(
                userMessage = userMessage,
            )
        }
    }

    private inner class ChatSessionHandler(
        val ui: ApplicationInterface,
        val session: Session,
        val user: User?,
        val api: API
    ) {
        val messageHistory: MutableList<String> = mutableListOf()

        fun handleUserMessage(userMessage: String) {
            try {
                messageHistory.add(userMessage)
                val planSettings = (getSettings(session, user, PlanSettings::class.java) ?: PlanSettings(
                    model = model,
                    parsingModel = parsingModel,
                    command = planSettings.command,
                    temperature = planSettings.temperature,
                    workingDir = planSettings.workingDir,
                    env = planSettings.env
                )).copy(
                    allowBlocking = false,
                )
                if (api is ChatClient) api.budget = planSettings.budget
                val coordinator = PlanCoordinator(
                    user = user,
                    session = session,
                    dataStorage = dataStorage,
                    ui = ui,
                    root = rootFile.toPath(),
                    planSettings = planSettings
                )
                val mainTask = coordinator.ui.newTask()
                val sessionTask = ui.newTask(false).apply { mainTask.verbose(placeholder) }
                val plan = PlanCoordinator.initialPlan(
                    codeFiles = coordinator.codeFiles,
                    files = coordinator.files,
                    root = coordinator.root,
                    task = sessionTask,
                    userMessage = userMessage,
                    ui = coordinator.ui,
                    planSettings = coordinator.planSettings,
                    api = api
                )
                val modifiedPlan = addRespondToChatTask(plan.plan)
                val planProcessingState = coordinator.executePlan(
                        plan = modifiedPlan,
                        task = sessionTask,
                        userMessage = userMessage,
                        api = api
                    )
                val response = planProcessingState.taskResult["respond_to_chat"] as? String
                if (response != null) {
                    mainTask.add(MarkdownUtil.renderMarkdown(response, ui = ui))
                    messageHistory.add(response)
                } else {
                    mainTask.add("Sorry, I couldn't generate a response.")
                    messageHistory.add("Sorry, I couldn't generate a response.")
                }
                mainTask.complete()
            } catch (e: Throwable) {
                ui.newTask().error(ui, e)
                log.warn("Error", e)
            }
        }

    }

    private fun addRespondToChatTask(plan: PlanningTask.TaskBreakdownInterface): PlanningTask.TaskBreakdownInterface {
        val tasksByID = plan.tasksByID?.toMutableMap() ?: mutableMapOf()
        val respondTaskId = "respond_to_chat"

        tasksByID[respondTaskId] = PlanningTask.PlanTask(
            description = "Respond to the user's chat message based on the executed plan",
            taskType = TaskType.Inquiry,
            task_dependencies = tasksByID.keys.toList()
        )

        return PlanningTask.TaskBreakdownResult(
            tasksByID = tasksByID,
            finalTaskID = respondTaskId
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(PlanChatApp::class.java)
    }
}