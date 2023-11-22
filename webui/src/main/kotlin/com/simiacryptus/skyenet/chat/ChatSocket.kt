package com.simiacryptus.skyenet.chat

import com.simiacryptus.skyenet.session.SocketManager
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketAdapter

class ChatSocket(
    private val sessionState: SocketManager,
) : WebSocketAdapter() {


    override fun onWebSocketConnect(session: Session) {
        super.onWebSocketConnect(session)
        log.debug("{} - Socket connected: {}", session, session.remote)
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

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(ChatSocket::class.java)
    }
}


