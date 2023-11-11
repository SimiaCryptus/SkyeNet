package com.simiacryptus.skyenet.body

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketAdapter
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer

abstract class WebSocketServer(val resourceBase: String) {

    abstract val applicationName: String

    val stateCache: MutableMap<String, SessionInterface> = mutableMapOf()

    inner class MessageWebSocket(
        val sessionId: String,
        private val sessionState: SessionInterface,
    ) : WebSocketAdapter() {

        override fun onWebSocketConnect(session: Session) {
            super.onWebSocketConnect(session)
//            logger.debug("$sessionId - Socket connected: $session")
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
//            logger.debug("$sessionId - Socket closed: [$statusCode] $reason")
            sessionState.removeSocket(this)
        }

        override fun onWebSocketError(cause: Throwable) {
            super.onWebSocketError(cause)
//            logger.debug("$sessionId - WebSocket error: $cause")
        }

    }

    inner class WebSocketHandler : JettyWebSocketServlet() {
        override fun configure(factory: JettyWebSocketServletFactory) {
            factory.setCreator { req, resp ->
                val sessionId = req.parameterMap["sessionId"]?.firstOrNull()
                return@setCreator if (null == sessionId) {
//                    logger.warn("No session ID provided")
                    null
                } else {
//                    logger.debug("Creating socket for $sessionId")
                    MessageWebSocket(sessionId, stateCache.getOrPut(sessionId) {
//                        logger.debug("Creating session for $sessionId")
                        newSession(sessionId)
                    })
                }
            }
        }
    }

    abstract fun newSession(sessionId: String): SessionInterface

    fun start(port: Int = 8080, baseUrl: String = "http://localhost:$port"): Server {
        val server = Server(port)
        configure(server, baseUrl)
        server.start()
        return server
    }

    open fun configure(server: Server, baseUrl: String, path: String = "/") {
        val webAppContext = WebAppContext()
        webAppContext.baseResource = baseResource
        webAppContext.contextPath = path
        webAppContext.welcomeFiles = arrayOf("index.html")
        JettyWebSocketServletContainerInitializer.configure(webAppContext, null)
        configure(webAppContext, path, baseUrl)
        server.handler = webAppContext
    }

    open val baseResource: Resource? get() = Resource.newResource(javaClass.classLoader.getResource(resourceBase))

    open fun configure(webAppContext: WebAppContext, prefix: String = "/", baseUrl: String) {
        webAppContext.addServlet(ServletHolder(javaClass.simpleName + "/default", defaultServlet), prefix + "")
        webAppContext.addServlet(ServletHolder(javaClass.simpleName + "/ws", webSocketHandler), prefix + "ws/*")
        webAppContext.addServlet(ServletHolder(javaClass.simpleName + "/newSession", newSessionServlet),prefix + "newSession")
    }

    open val newSessionServlet get() = NewSessionServlet()

    open val webSocketHandler get() = WebSocketHandler()

    open val defaultServlet: DefaultServlet
        get() {
            val defaultServlet = DefaultServlet()
            //defaultServlet.dirAllowed = false
            return defaultServlet
        }

    companion object {
        val logger = org.slf4j.LoggerFactory.getLogger(WebSocketServer::class.java)
    }
}


