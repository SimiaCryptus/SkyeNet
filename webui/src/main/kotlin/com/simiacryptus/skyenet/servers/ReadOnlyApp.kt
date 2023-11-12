package com.simiacryptus.skyenet.servers

import com.simiacryptus.skyenet.sessions.*
import org.slf4j.LoggerFactory
import java.io.File

open class ReadOnlyApp(
    applicationName: String,
    temperature: Double = 0.3,
    oauthConfig: String? = null,
) : ApplicationBase(
    applicationName = applicationName,
    oauthConfig = oauthConfig,
    temperature = temperature,
    resourceBase = "readOnly",
) {

    companion object {
        val log = LoggerFactory.getLogger(ReadOnlyApp::class.java)
    }

    override fun newSession(sessionId: String): SessionInterface = ReadOnlySession(sessionId)

    class ReadOnlySession(sessionId: String) : SessionBase(sessionId) {
        override fun onWebSocketText(socket: MessageWebSocket, message: String) {
            // Do nothing
        }
    }


}