package com.simiacryptus.skyenet.apps.coding

import com.simiacryptus.jopenai.API
import com.simiacryptus.skyenet.core.platform.ClientManager
import com.simiacryptus.skyenet.interpreter.Interpreter
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import kotlin.reflect.KClass

open class CodingApp<T: Interpreter>(
        applicationName: String,
        private val interpreter: KClass<T>,
        open val symbols: Map<String, Any> = mapOf(),
        val temperature: Double = 0.1,
) : ApplicationServer(
    applicationName = applicationName,
) {
    override fun userMessage(
        session: Session,
        user: User?,
        userMessage: String,
        ui: ApplicationInterface,
        api: API
    ) {
        (api as ClientManager.MonitoredClient).budget = 2.00
        CodingAgent(
            api = api,
            dataStorage = dataStorage,
            session = session,
            user = user,
            ui = ui,
            interpreter = interpreter,
            symbols = symbols,
            temperature = temperature,
        ).start(
            userMessage = userMessage,
        )
    }
}