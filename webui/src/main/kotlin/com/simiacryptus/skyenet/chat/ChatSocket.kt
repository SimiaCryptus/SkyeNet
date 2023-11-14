package com.simiacryptus.skyenet.chat

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.servlet.AuthenticatedWebsite
import com.simiacryptus.skyenet.util.UsageManager.incrementUsage
import com.simiacryptus.skyenet.session.SessionDataStorage
import com.simiacryptus.skyenet.session.SessionInterface
import com.simiacryptus.skyenet.util.AuthorizationManager
import com.simiacryptus.skyenet.util.AuthorizationManager.OperationType
import com.simiacryptus.skyenet.util.AuthorizationManager.isAuthorized
import com.simiacryptus.skyenet.util.UserSettingsManager.getUserSettings
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketAdapter
import org.slf4j.event.Level

class ChatSocket(
    val sessionId: String,
    private val sessionState: SessionInterface,
    private val authId: String?,
    val sessionDataStorage: SessionDataStorage?,
) : WebSocketAdapter() {

    val user get() = if (authId == null) null else AuthenticatedWebsite.users[authId]

    val userApi: OpenAIClient?
        get() {
            val user = user
            val userSettings = if (user == null) null else getUserSettings(user.id)
            return if (userSettings == null) null else {
                if (userSettings.apiKey.isBlank()) null else object : OpenAIClient(
                    key = userSettings.apiKey,
                    logLevel = Level.DEBUG,
                    logStreams = mutableListOf(
                        sessionDataStorage?.getSessionDir(sessionId)?.resolve("openai.log")?.outputStream()?.buffered()
                    ).filterNotNull().toMutableList(),
                ) {
                    override fun incrementTokens(model: Model?, tokens: Int) {
                        incrementUsage(sessionId, user?.id, model!!, tokens)
                        super.incrementTokens(model, tokens)
                    }
                }
            }
        }


    val api: OpenAIClient
        get() {
            val user = user
            val userApi = userApi
            if (userApi != null) return userApi
            val canUseGlobalKey = isAuthorized(null, user?.email, OperationType.GlobalKey)
            if (!canUseGlobalKey) throw RuntimeException("No API key")
            return object : OpenAIClient(
                logLevel = Level.DEBUG,
                logStreams = mutableListOf(
                    sessionDataStorage?.getSessionDir(sessionId)?.resolve("openai.log")?.outputStream()?.buffered()
                ).filterNotNull().toMutableList()
            ) {
                override fun incrementTokens(model: Model?, tokens: Int) {
                    if(null != model) incrementUsage(sessionId, user?.id, model, tokens)
                    super.incrementTokens(model, tokens)
                }
            }
        }

    override fun onWebSocketConnect(session: Session) {
        super.onWebSocketConnect(session)
        ChatServer.log.debug("{} - Socket connected: {}", sessionId, session.remote)
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
