package com.simiacryptus.skyenet.body

import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
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
import java.util.*

abstract class WebSocketServer(val resourceBase: String) {

    inner class NewSessionServlet : HttpServlet() {
        override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
            val sessionId = UUID.randomUUID().toString()
            resp.contentType = "text/plain"
            resp.status = HttpServletResponse.SC_OK
            resp.writer.write(sessionId)
        }
    }

    val stateCache: MutableMap<String, SessionInterface> = mutableMapOf()

    inner class MessageWebSocket(
        val sessionId: String,
        val sessionState: SessionInterface,
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

    fun start(port: Int = 8080): Server {
        val server = Server(port)
        configure(server)
        server.start()
        return server
    }

    open fun configure(server: Server) {
        val webAppContext = WebAppContext()
        webAppContext.baseResource = baseResource
        webAppContext.contextPath = "/"
        webAppContext.welcomeFiles = arrayOf("index.html")
        configure(webAppContext)
        server.handler = webAppContext
    }

    open val baseResource: Resource? get() = Resource.newResource(javaClass.classLoader.getResource(resourceBase))

    open fun configure(webAppContext: WebAppContext) {
        JettyWebSocketServletContainerInitializer.configure(webAppContext, null)
        val defaultServlet = DefaultServlet()
        //defaultServlet.dirAllowed = false
        webAppContext.addServlet(ServletHolder("default", defaultServlet), "/")
        webAppContext.addServlet(ServletHolder("ws", WebSocketHandler()), "/ws/*")
        webAppContext.addServlet(ServletHolder("newSession", NewSessionServlet()), "/newSession")
    }

    companion object {
        val logger = org.slf4j.LoggerFactory.getLogger(WebSocketServer::class.java)
    }
}


