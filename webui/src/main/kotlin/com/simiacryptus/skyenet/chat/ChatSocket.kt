package com.simiacryptus.skyenet.chat

import com.simiacryptus.openai.models.OpenAIModel
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.platform.*
import com.simiacryptus.skyenet.platform.ApplicationServices.authorizationManager
import com.simiacryptus.skyenet.session.SocketManager
import com.simiacryptus.skyenet.platform.AuthorizationManager.OperationType.GlobalKey
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketAdapter
import org.slf4j.event.Level

class ChatSocket(
    private val session: com.simiacryptus.skyenet.platform.Session,
    private val sessionState: SocketManager,
    private val dataStorage: DataStorage?,
    private val user: User?,
) : WebSocketAdapter() {

    private val logfile = ".sys/openai.log"

    val api: OpenAIClient
        get() {
            val user = user
            val userApi = userApi
            if (userApi != null) return userApi
            val canUseGlobalKey = authorizationManager.isAuthorized(null, user, GlobalKey)
            if (!canUseGlobalKey) throw RuntimeException("No API key")
            return object : OpenAIClient(
                logLevel = Level.DEBUG,
                logStreams = mutableListOf(
                    dataStorage?.getSessionDir(user, session)?.resolve(logfile)?.outputStream()?.buffered()
                ).filterNotNull().toMutableList()
            ) {
                override fun incrementTokens(model: OpenAIModel?, tokens: Usage) {
                    if(null != model) ApplicationServices.usageManager.incrementUsage(session, user, model, tokens)
                    super.incrementTokens(model, tokens)
                }
            }
        }

    private val userApi: OpenAIClient?
        get() {
            val user = user
            val userSettings = if (user == null) null else ApplicationServices.userSettingsManager.getUserSettings(user)
            return if (userSettings == null) null else {
                if (userSettings.apiKey.isBlank()) null else object : OpenAIClient(
                    key = userSettings.apiKey,
                    logLevel = Level.DEBUG,
                    logStreams = mutableListOf(
                        dataStorage?.getSessionDir(user, session)?.resolve(logfile)?.outputStream()?.buffered()
                    ).filterNotNull().toMutableList(),
                ) {
                    override fun incrementTokens(model: OpenAIModel?, tokens: Usage) {
                        ApplicationServices.usageManager.incrementUsage(session, user, model!!, tokens)
                        super.incrementTokens(model, tokens)
                    }
                }
            }
        }


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
