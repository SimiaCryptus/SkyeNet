package com.simiacryptus.skyenet.servers

import com.simiacryptus.skyenet.sessions.*
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

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

    private val messageVersions = HashMap<String, HashMap<String, AtomicInteger>>()

    inner class ReadOnlySession(sessionId: String) : SessionBase(sessionId, sessionDataStorage.loadMessages(sessionId).entries.map {
        "${it.key},${
            messageVersions.computeIfAbsent(sessionId) { HashMap() }.computeIfAbsent(it.key) { AtomicInteger(1) }.get()
        },${it.value}"
    }.toMutableList()) {
        override fun onWebSocketText(socket: MessageWebSocket, message: String) {
            // Do nothing
        }
    }


}