package com.simiacryptus.skyenet.apps.general

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.models.ChatModel
import com.simiacryptus.skyenet.apps.plan.PlanCoordinator
import com.simiacryptus.skyenet.apps.plan.PlanCoordinator.Companion.initialPlan
import com.simiacryptus.skyenet.apps.plan.PlanSettings
import com.simiacryptus.skyenet.apps.plan.TaskBreakdownWithPrompt
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.ApplicationServicesConfig.dataStorageRoot
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationSocketManager
import com.simiacryptus.skyenet.webui.session.SocketManager
import com.simiacryptus.util.JsonUtil
import org.slf4j.LoggerFactory
import java.io.File

open class PlanAheadApp(
  applicationName: String = "Task Planning v1.1",
  path: String = "/taskDev",
  val planSettings: PlanSettings,
  val model: ChatModel,
  val parsingModel: ChatModel,
  val domainName: String = "localhost",
  showMenubar: Boolean = true,
  val initialPlan: TaskBreakdownWithPrompt? = null,
  val api: API? = null,
  val api2: OpenAIClient,
) : ApplicationServer(
  applicationName = applicationName,
  path = path,
  showMenubar = showMenubar,
  root = planSettings.workingDir?.let { File(it) } ?: dataStorageRoot,
) {
  override val singleInput = true

  @Suppress("UNCHECKED_CAST")
  override fun <T : Any> initSettings(session: Session): T = planSettings.let {
    if (null == root) it.copy(workingDir = root.absolutePath) else
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
            root = planSettings?.workingDir?.let { File(it).toPath() } ?: dataStorage.getDataDir(user, session).toPath(),
            planSettings = planSettings!!
          )
          coordinator.executeTaskBreakdownWithPrompt(JsonUtil.toJson(initialPlan), api!!, api2, ui.newTask())
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
        root = planSettings?.workingDir?.let { File(it).toPath() } ?: dataStorage.getDataDir(user, session).toPath(),
        planSettings = planSettings!!
      )
      val task = ui.newTask()
      val plan = initialPlan(
        codeFiles = coordinator.codeFiles,
        files = coordinator.files,
        root = coordinator.root,
        task = task,
        userMessage = userMessage,
        ui = coordinator.ui,
        planSettings = coordinator.planSettings,
        api = api
      )
      coordinator.executePlan(plan.plan, task, userMessage = userMessage, api = api, api2 = api2)
    } catch (e: Throwable) {
      ui.newTask().error(ui, e)
      log.warn("Error", e)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(PlanAheadApp::class.java)
  }
}