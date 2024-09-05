package com.simiacryptus.skyenet.apps.general

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.models.OpenAITextModel
import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.apps.plan.PlanCoordinator
import com.simiacryptus.skyenet.apps.plan.PlanSettings
import com.simiacryptus.skyenet.apps.plan.PlanUtil
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationSocketManager
import com.simiacryptus.skyenet.webui.session.SocketManager
import org.slf4j.LoggerFactory
import java.io.File

class PlanAheadApp(
    applicationName: String = "Task Planning v1.1",
    path: String = "/taskDev",
     val rootFile: File,
    val planSettings: PlanSettings,
    val model: OpenAITextModel,
    val parsingModel: OpenAITextModel,
    val domainName : String = "localhost",
    showMenubar: Boolean = true,
    val initialPlan: PlanUtil.TaskBreakdownWithPrompt? = null,
    val api: API? = null,
) : ApplicationServer(
    applicationName = applicationName,
    path = path,
    showMenubar = showMenubar,
) {
    override val singleInput: Boolean get() = true
    override val root: File get() = rootFile

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> initSettings(session: Session): T = planSettings.let {
        if (null == rootFile) it.copy(workingDir = root.absolutePath) else
        it
    } as T

    override fun newSession(user: User?, session: Session): SocketManager {
        val socketManager = super.newSession(user, session)
        val ui = (socketManager as ApplicationSocketManager).applicationInterface
        if (initialPlan != null) {
            socketManager.pool.submit {
                try {
                    val planSettings = getSettings(session, user, PlanSettings::class.java)
                    if (api is ChatClient) api.budget = planSettings?.budget
                    val coordinator = PlanCoordinator(
                        user = user,
                        session = session,
                        dataStorage = dataStorage,
                        ui = ui,
                        root = rootFile.toPath(),
                        planSettings = planSettings!!
                    )
                    coordinator.executeTaskBreakdownWithPrompt(JsonUtil.toJson(initialPlan), api!!)
                } catch (e: Throwable) {
                    ui.newTask().error(ui, e)
                    log.warn("Error", e)
                }
            }
        }
        return socketManager
    }

    override fun userMessage(
        session: Session,
        user: User?,
        userMessage: String,
        ui: ApplicationInterface,
        api: API
    ) {
        try {
            val planSettings = getSettings(session, user, PlanSettings::class.java)
            if (api is ChatClient) api.budget = planSettings?.budget ?: 2.0
            val coordinator = PlanCoordinator(
                user = user,
                session = session,
                dataStorage = dataStorage,
                ui = ui,
                root = rootFile.toPath(),
                planSettings = planSettings!!
            )
            coordinator.startProcess(userMessage = userMessage, api = api)
        } catch (e: Throwable) {
            ui.newTask().error(ui, e)
            log.warn("Error", e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(PlanAheadApp::class.java)
    }
}