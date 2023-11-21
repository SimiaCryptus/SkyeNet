package com.simiacryptus.skyenet.chat

import com.simiacryptus.skyenet.platform.*
import com.simiacryptus.skyenet.platform.ApplicationServices.authenticationManager
import com.simiacryptus.skyenet.platform.AuthenticationManager.Companion.COOKIE_NAME
import com.simiacryptus.skyenet.servlet.NewSessionServlet
import com.simiacryptus.skyenet.session.SessionInterface
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.websocket.server.JettyServerUpgradeRequest
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory

abstract class ChatServer(val resourceBase: String) {

    abstract val applicationName: String
    open val dataStorage: DataStorage? = null

    inner class WebSocketHandler : JettyWebSocketServlet() {
        val stateCache: MutableMap<SessionID, SessionInterface> = mutableMapOf()
        override fun configure(factory: JettyWebSocketServletFactory) {
            factory.setCreator { req, resp ->
                try {
                    val authId = req.getCookie(COOKIE_NAME)
                    return@setCreator if (!req.parameterMap.containsKey("sessionId")) {
                        null
                    } else {
                        val sessionId = SessionID(req.parameterMap["sessionId"]?.first()!!)
                        val sessionState: SessionInterface = getSession(sessionId, req)
                        val user = authenticationManager.getUser(authId)
                        ChatSocket(sessionId, sessionState, dataStorage, user)
                    }
                } catch (e: Exception) {
                    log.warn("Error configuring websocket", e)
                }
            }
        }

        private fun getSession(
            sessionId: SessionID,
            req: JettyServerUpgradeRequest
        ) = if (stateCache.containsKey(sessionId)) {
            stateCache[sessionId]!!
        } else {
            val user = authenticationManager.getUser(req.getCookie(COOKIE_NAME))
            val sessionState = newSession(user, sessionId)
            stateCache[sessionId] = sessionState
            sessionState
        }
    }

    abstract fun newSession(userId: UserInfo?, sessionId: SessionID): SessionInterface

    open val baseResource: Resource? get() = Resource.newResource(javaClass.classLoader.getResource(resourceBase))
    private val newSessionServlet by lazy { NewSessionServlet() }
    private val webSocketHandler by lazy { WebSocketHandler() }
    private val defaultServlet by lazy { DefaultServlet() }

    open fun configure(webAppContext: WebAppContext, path: String = "/", baseUrl: String) {
        webAppContext.addServlet(ServletHolder(javaClass.simpleName + "/default", defaultServlet), "/")
        webAppContext.addServlet(ServletHolder(javaClass.simpleName + "/ws", webSocketHandler), "/ws")
        webAppContext.addServlet(ServletHolder(javaClass.simpleName + "/newSession", newSessionServlet),"/newSession")
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(ChatServer::class.java)
        fun JettyServerUpgradeRequest.getCookie(name: String) = cookies?.find { it.name == name }?.value
    }
}


