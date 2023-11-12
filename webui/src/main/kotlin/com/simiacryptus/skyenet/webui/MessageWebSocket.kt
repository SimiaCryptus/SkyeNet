package com.simiacryptus.skyenet.webui

import com.simiacryptus.openai.OpenAIClient
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketAdapter
import org.slf4j.event.Level
import java.net.HttpCookie

class MessageWebSocket(
    val sessionId: String,
    private val sessionState: SessionInterface,
    val authId: String?,
    val sessionDataStorage: SessionDataStorage,
) : WebSocketAdapter() {

    val api: OpenAIClient get() {
        val client = OpenAIClient(
            logLevel = Level.DEBUG,
            logStreams = mutableListOf(
                sessionDataStorage.getSessionDir(sessionId).resolve("openai.log").outputStream().buffered()
            )
        )
        return client
    }

    override fun onWebSocketConnect(session: Session) {
        super.onWebSocketConnect(session)
        WebSocketServer.log.debug("{} - Socket connected: {}", sessionId, session.remote)
        sessionState.addSocket(this)
        sessionState.getReplay().forEach {
            try {
                remote.sendString(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onWebSocketText(message: String) {
        super.onWebSocketText(message)
        sessionState.onWebSocketText(this, message)
    }

    override fun onWebSocketClose(statusCode: Int, reason: String?) {
        super.onWebSocketClose(statusCode, reason)
        sessionState.removeSocket(this)
    }

}