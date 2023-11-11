package com.simiacryptus.skyenet.webui

import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketAdapter

class MessageWebSocket(
    val sessionId: String,
    private val sessionState: SessionInterface,
) : WebSocketAdapter() {

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