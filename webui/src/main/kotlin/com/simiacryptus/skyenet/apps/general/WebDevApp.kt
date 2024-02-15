package com.simiacryptus.skyenet.apps.general

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.skyenet.core.platform.ClientManager
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer

open class WebDevApp(
        applicationName: String = "Web Dev Assistant v1.0",
        open val symbols: Map<String, Any> = mapOf(),
        val temperature: Double = 0.1,
) : ApplicationServer(
    applicationName = applicationName,
    path = "/webdev",
) {
    override fun userMessage(
      session: Session,
      user: User?,
      userMessage: String,
      ui: ApplicationInterface,
      api: API
    ) {
        val settings = getSettings(session, user) ?: Settings()
        (api as ClientManager.MonitoredClient).budget = settings.budget ?: 2.00
        WebDevAgent(
          api = api,
          dataStorage = dataStorage,
          session = session,
          user = user,
          ui = ui,
          tools = settings.tools,
          model = settings.model,
        ).start(
            userMessage = userMessage,
        )
    }

    data class Settings(
      val budget: Double? = 2.00,
      val tools : List<String> = emptyList(),
      val model : ChatModels = ChatModels.GPT35Turbo,
    )

    override val settingsClass: Class<*> get() = Settings::class.java
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> initSettings(session: Session): T? = Settings() as T
}