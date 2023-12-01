package com.simiacryptus.skyenet.webui.chat

import com.simiacryptus.skyenet.core.platform.*
import com.simiacryptus.skyenet.core.platform.ApplicationServices.authenticationManager
import com.simiacryptus.skyenet.webui.servlet.NewSessionServlet
import com.simiacryptus.skyenet.webui.session.SocketManager
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.websocket.server.JettyServerUpgradeRequest
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory

abstract class ChatServer(val resourceBase: String) {

    abstract val applicationName: String
    open val dataStorage: StorageInterface? = null

    inner class WebSocketHandler : JettyWebSocketServlet() {
        private val stateCache: MutableMap<Session, SocketManager> = mutableMapOf()
        override fun configure(factory: JettyWebSocketServletFactory) {
            factory.setCreator { req, resp ->
                try {
                    return@setCreator if (!req.parameterMap.containsKey("sessionId")) {
                        throw IllegalArgumentException("sessionId is required")
                    } else {
                        val session = Session(req.parameterMap["sessionId"]?.first()!!)
                        ChatSocket(
                            if (stateCache.containsKey(session)) {
                                stateCache[session]!!
                            } else {
                                val user = authenticationManager.getUser(req.getCookie(AuthenticationInterface.AUTH_COOKIE))
                                val sessionState = newSession(user, session)
                                stateCache[session] = sessionState
                                sessionState
                            }
                        )
                    }
                } catch (e: Exception) {
                    log.warn("Error configuring websocket", e)
                }
            }
        }
    }

    abstract fun newSession(user: User?, session: Session): SocketManager

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


