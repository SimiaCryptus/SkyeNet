package com.simiacryptus.skyenet.body

import java.awt.Desktop
import java.net.URI

object TestWebSocketServer {
    @JvmStatic
    fun main(args: Array<String>) {
        val port = 8080
        val server = (object : WebSocketServer("simple") {
            override fun newSession(sessionId: String): SessionState {
                return object : SessionState(sessionId) {
                    override fun onWebSocketText(socket: MessageWebSocket, message: String) {
                        logger.info("$sessionId - Received message: $message")
                        send("Server: $message")
                    }
                }
            }
        }).start(port)
        Desktop.getDesktop().browse(URI("http://localhost:$port/"))
        server.join()
    }
}