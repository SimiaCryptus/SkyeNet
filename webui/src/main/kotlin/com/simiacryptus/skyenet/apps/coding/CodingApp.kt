package com.simiacryptus.skyenet.apps.coding

import com.simiacryptus.jopenai.API
import com.simiacryptus.skyenet.core.Interpreter
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import kotlin.reflect.KClass

open class CodingApp<T:Interpreter>(
        applicationName: String,
        private val interpreter: KClass<T>,
        private val symbols: Map<String, Any>,
        temperature: Double = 0.1,
) : ApplicationServer(
    applicationName = applicationName,
) {
    override fun newSession(
        session: Session,
        user: User?,
        userMessage: String,
        ui: ApplicationInterface,
        api: API
    ) {
        CodingAgent(
            api = api,
            dataStorage = dataStorage,
            session = session,
            user = user,
            ui = ui,
            interpreter = interpreter,
            symbols = symbols,
        ).start(
            userMessage = userMessage,
        )
    }
}