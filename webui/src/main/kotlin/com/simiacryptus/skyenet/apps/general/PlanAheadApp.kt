package com.simiacryptus.skyenet.apps.general

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.models.OpenAITextModel
import com.simiacryptus.skyenet.apps.plan.PlanCoordinator
import com.simiacryptus.skyenet.apps.plan.PlanSettings
import com.simiacryptus.skyenet.core.platform.ClientManager
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import org.slf4j.LoggerFactory
import java.io.File

class PlanAheadApp(
    applicationName: String = "Task Planning v1.1",
    path: String = "/taskDev",
    val rootFile: File?,
    val planSettings: PlanSettings,
    val model: OpenAITextModel,
    val parsingModel: OpenAITextModel,
    val domainName : String = "localhost",
    showMenubar: Boolean = true,
) : ApplicationServer(
    applicationName = applicationName,
    path = path,
    showMenubar = showMenubar,
) {
    override val root: File get() = rootFile ?: super.root

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> initSettings(session: Session): T = planSettings.let {
        if (null == rootFile) it.copy(workingDir = root.absolutePath) else
        it
    } as T

    override fun userMessage(
        session: Session,
        user: User?,
        userMessage: String,
        ui: ApplicationInterface,
        api: API
    ) {
        try {
            val planSettings = getSettings<PlanSettings>(session, user)
            if (api is ClientManager.MonitoredClient) api.budget = planSettings?.budget ?: 2.0
            PlanCoordinator(
                user = user,
                session = session,
                dataStorage = dataStorage,
                api = api,
                ui = ui,
                root = (rootFile ?: dataStorage.getDataDir(user, session)).toPath(),
                planSettings = planSettings!!
            ).startProcess(userMessage = userMessage)
        } catch (e: Throwable) {
            ui.newTask().error(ui, e)
            log.warn("Error", e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(PlanAheadApp::class.java)
    }
}