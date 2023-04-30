package com.simiacryptus.skyenet.body

import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketAdapter
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer
import java.util.*

abstract class WebSocketServer(private val resourceBase: String) {

    inner class NewSessionServlet : HttpServlet() {
        override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
            val sessionId = UUID.randomUUID().toString()
            resp.contentType = "text/plain"
            resp.status = HttpServletResponse.SC_OK
            resp.writer.write(sessionId)
        }
    }

    abstract class SessionState(val sessionId: String) {
        private val sockets: MutableList<MessageWebSocket> = mutableListOf()
        protected fun publish(
            out: String,
        ) {
            val socketsSnapshot = sockets.toTypedArray()
            socketsSnapshot.forEach {
                try {
                    it.remote.sendString(out)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun removeSocket(socket: MessageWebSocket) {
            sockets.remove(socket)
        }

        fun addSocket(socket: MessageWebSocket) {
            sockets.add(socket)
        }

        private val sentMessages: MutableList<String> = mutableListOf()

        open fun send(out: String) {
            sentMessages.add(out)
            publish(out)
        }

        open fun getReplay(): List<String> {
            return sentMessages
        }

        abstract fun onWebSocketText(socket: MessageWebSocket, message: String)

    }

    abstract class SessionStateByID(
        sessionId: String,
        val sentMessages: LinkedHashMap<String, String> = LinkedHashMap<String, String>(),
    ) : SessionState(sessionId) {


        override fun send(out: String) {
            try {
                logger.debug("$sessionId - $out")
                val split = out.split(',', ignoreCase = false, limit = 2)
                setMessage(split[0], split[1])
                publish(out)
            } catch (e: Exception) {
                logger.debug("$sessionId - $out", e)
            }
        }

        protected open fun setMessage(key: String, value: String) {
            sentMessages.put(key, value)
        }

        override fun getReplay(): List<String> {
            return sentMessages.entries.map { "${it.key},${it.value}" }
        }
    }

    val stateCache: MutableMap<String, SessionState> = mutableMapOf()

    inner class MessageWebSocket(
        val sessionId: String,
        val sessionState: SessionState,
    ) : WebSocketAdapter() {

        override fun onWebSocketConnect(session: Session) {
            super.onWebSocketConnect(session)
            logger.debug("$sessionId - Socket connected: $session")
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
            logger.debug("$sessionId - Socket closed: [$statusCode] $reason")
            sessionState.removeSocket(this)
        }

        override fun onWebSocketError(cause: Throwable) {
            super.onWebSocketError(cause)
            logger.debug("$sessionId - WebSocket error: $cause")
        }
    }

    inner class WebSocketHandler : JettyWebSocketServlet() {
        override fun configure(factory: JettyWebSocketServletFactory) {
            factory.setCreator { req, resp ->
                val sessionId = req.parameterMap["sessionId"]?.firstOrNull()
                if (null == sessionId) {
                    logger.debug("No session ID provided")
                    return@setCreator null
                }
                MessageWebSocket(sessionId, stateCache.getOrPut(sessionId) { newSession(sessionId) })
            }
        }
    }

    abstract fun newSession(sessionId: String): SessionState

    fun start(port: Int = 8080): Server {
        val server = Server(port)
        configure(server)
        server.start()
        return server
    }

    open fun configure(server: Server) {
        val webAppContext = WebAppContext()
        webAppContext.baseResource = Resource.newClassPathResource(resourceBase)
        webAppContext.contextPath = "/"
        webAppContext.welcomeFiles = arrayOf("index.html")
        configure(webAppContext)
        server.handler = webAppContext
    }

    open fun configure(webAppContext: WebAppContext) {
        JettyWebSocketServletContainerInitializer.configure(webAppContext, null)
        val webSocketServletHolder = ServletHolder("ws", WebSocketHandler())
        webAppContext.addServlet(webSocketServletHolder, "/ws/*")
        val newSessionServletHolder = ServletHolder("newSession", NewSessionServlet())
        webAppContext.addServlet(newSessionServletHolder, "/newSession")
    }

    companion object {
        val logger = org.slf4j.LoggerFactory.getLogger(WebSocketServer::class.java)
    }
}


