package com.simiacryptus.skyenet.sessions

import com.simiacryptus.skyenet.servlet.NewSessionServlet
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer

abstract class WebSocketServer(val resourceBase: String) {

    abstract val applicationName: String
    abstract val sessionDataStorage: SessionDataStorage

    val stateCache: MutableMap<String, SessionInterface> = mutableMapOf()

    inner class WebSocketHandler : JettyWebSocketServlet() {
        override fun configure(factory: JettyWebSocketServletFactory) {
            factory.setCreator { req, resp ->
                val sessionId = req.parameterMap["sessionId"]?.firstOrNull()
                val authId = req.cookies?.find { it.name == "sessionId" }?.value
                return@setCreator if (null == sessionId) {
                    null
                } else {
                    MessageWebSocket(sessionId, stateCache.getOrPut(sessionId) {
                        newSession(sessionId)
                    }, authId, sessionDataStorage)
                }
            }
        }
    }

    abstract fun newSession(sessionId: String): SessionInterface

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
    protected val newSessionServlet by lazy { NewSessionServlet() }
    protected val webSocketHandler by lazy { WebSocketHandler() }
    protected val defaultServlet by lazy { DefaultServlet() }

    open fun configure(webAppContext: WebAppContext, prefix: String = "/", baseUrl: String) {
        webAppContext.addServlet(ServletHolder(javaClass.simpleName + "/default", defaultServlet), prefix + "")
        webAppContext.addServlet(ServletHolder(javaClass.simpleName + "/ws", webSocketHandler), prefix + "ws/*")
        webAppContext.addServlet(ServletHolder(javaClass.simpleName + "/newSession", newSessionServlet),prefix + "newSession")
    }


    companion object {
        val log = org.slf4j.LoggerFactory.getLogger(WebSocketServer::class.java)
    }
}


