package com.simiacryptus.skyenet.chat

import com.simiacryptus.skyenet.config.ApplicationServices
import com.simiacryptus.skyenet.config.AuthenticationManager
import com.simiacryptus.skyenet.config.DataStorage
import com.simiacryptus.skyenet.servlet.NewSessionServlet
import com.simiacryptus.skyenet.session.SessionInterface
import com.simiacryptus.util.JsonUtil
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.websocket.server.JettyServerUpgradeRequest
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory

abstract class ChatServer(val resourceBase: String) {

    fun JettyServerUpgradeRequest.getCookie(name: String) = cookies?.find { it.name == name }?.value

    abstract val applicationName: String
    open val dataStorage: DataStorage? = null
    val stateCache: MutableMap<String, SessionInterface> = mutableMapOf()

    inner class WebSocketHandler : JettyWebSocketServlet() {
        override fun configure(factory: JettyWebSocketServletFactory) {
            factory.setCreator { req, resp ->
                try {
                    val sessionId = req.parameterMap["sessionId"]?.firstOrNull()
                    val authId = req.getCookie(AuthenticationManager.COOKIE_NAME)
                    return@setCreator if (null == sessionId) {
                        null
                    } else {
                        val sessionState: SessionInterface
                        if (stateCache.containsKey(sessionId)) {
                            sessionState = stateCache[sessionId]!!
                        } else {
                            sessionState = newSession(
                                ApplicationServices.authenticationManager.getUser(
                                req.getCookie(AuthenticationManager.COOKIE_NAME)
                            )?.id, sessionId)
                            stateCache[sessionId] = sessionState
                        }
                        ChatSocket(sessionId, sessionState, dataStorage, ApplicationServices.authenticationManager.getUser(authId))
                    }
                } catch (e: Exception) {
                    log.warn("Error configuring websocket", e)
                }
            }
        }
    }

    inner class AppInfoServlet : HttpServlet() {
        override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
            resp.contentType = "text/json"
            resp.status = HttpServletResponse.SC_OK
            resp.writer.write(
                JsonUtil.objectMapper().writeValueAsString(
                    mapOf(
                        "applicationName" to applicationName
                    )
                )
            )
        }
    }

    abstract fun newSession(userId: String?, sessionId: String): SessionInterface

    open val baseResource: Resource? get() = Resource.newResource(javaClass.classLoader.getResource(resourceBase))
    protected val newSessionServlet by lazy { NewSessionServlet() }
    protected val webSocketHandler by lazy { WebSocketHandler() }
    protected val defaultServlet by lazy { DefaultServlet() }

    open fun configure(webAppContext: WebAppContext, path: String = "/", baseUrl: String) {
        webAppContext.addServlet(ServletHolder(javaClass.simpleName + "/default", defaultServlet), "/")
        webAppContext.addServlet(ServletHolder(javaClass.simpleName + "/ws", webSocketHandler), "/ws")
        webAppContext.addServlet(ServletHolder(javaClass.simpleName + "/newSession", newSessionServlet),"/newSession")
    }

    companion object {
        val log = org.slf4j.LoggerFactory.getLogger(ChatServer::class.java)
    }
}


