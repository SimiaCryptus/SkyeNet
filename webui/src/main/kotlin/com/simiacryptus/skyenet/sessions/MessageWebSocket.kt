package com.simiacryptus.skyenet.sessions

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.servlet.AuthenticatedWebsite
import com.simiacryptus.skyenet.servlet.UsageServlet.Companion.incrementUsage
import com.simiacryptus.skyenet.servlet.UserSettingsServlet.Companion.getUserSettings
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketAdapter
import org.slf4j.event.Level

class MessageWebSocket(
    val sessionId: String,
    private val sessionState: SessionInterface,
    val authId: String?,
    val sessionDataStorage: SessionDataStorage,
) : WebSocketAdapter() {

    val api: OpenAIClient
        get() {
            val userinfo = AuthenticatedWebsite.users[authId]
            val userApi: OpenAIClient? = if (userinfo == null) null else {
                val userSettings = getUserSettings(userinfo)
                if (userSettings.apiKey.isBlank()) null else object : OpenAIClient(
                    key = userSettings.apiKey,
                    logLevel = Level.DEBUG,
                    logStreams = mutableListOf(
                        sessionDataStorage.getSessionDir(sessionId).resolve("openai.log").outputStream().buffered()
                    ),
                ) {
                    override fun incrementTokens(model: Model?, tokens: Int) {
                        incrementUsage(sessionId, userinfo, model!!, tokens)
                        super.incrementTokens(model, tokens)
                    }
                }
            }
            return userApi ?: object : OpenAIClient(
                logLevel = Level.DEBUG,
                logStreams = mutableListOf(
                    sessionDataStorage.getSessionDir(sessionId).resolve("openai.log").outputStream().buffered()
                )
            ) {
                override fun incrementTokens(model: Model?, tokens: Int) {
                    incrementUsage(sessionId, userinfo, model!!, tokens)
                    super.incrementTokens(model, tokens)
                }
            }
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