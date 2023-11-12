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

    override fun newSession(sessionId: String): SessionInterface = object : SessionInterface {
        override fun removeSocket(socket: MessageWebSocket) {
            // Do nothing
        }

        override fun addSocket(socket: MessageWebSocket) {
            // Do nothing
        }

        override fun getReplay(): List<String> {
            return SessionDataStorage(File(File(".skynet"), applicationName)).loadMessages(sessionId).values.toList()
        }

        override fun onWebSocketText(socket: MessageWebSocket, message: String) {
            // Do nothing
        }
    }


}