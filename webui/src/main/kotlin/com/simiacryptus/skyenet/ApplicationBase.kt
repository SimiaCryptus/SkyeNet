package com.simiacryptus.skyenet

import com.simiacryptus.skyenet.servlet.AppInfoServlet
import com.simiacryptus.skyenet.chat.ChatServer
import com.simiacryptus.skyenet.chat.ChatSocket
import com.simiacryptus.skyenet.platform.ApplicationServices.authenticationManager
import com.simiacryptus.skyenet.platform.ApplicationServices.authorizationManager
import com.simiacryptus.skyenet.platform.ApplicationServices.dataStorageFactory
import com.simiacryptus.skyenet.servlet.*
import com.simiacryptus.skyenet.platform.AuthenticationManager.Companion.COOKIE_NAME
import com.simiacryptus.skyenet.session.SessionDiv
import com.simiacryptus.skyenet.session.SessionInterface
import com.simiacryptus.skyenet.platform.AuthorizationManager
import com.simiacryptus.skyenet.platform.SessionID
import com.simiacryptus.skyenet.platform.UserInfo
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.servlet.FilterHolder
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.webapp.WebAppContext
import org.slf4j.LoggerFactory
import java.io.File

abstract class ApplicationBase(
    final override val applicationName: String,
    resourceBase: String = "simpleSession",
    val temperature: Double = 0.1,
) : ChatServer(resourceBase) {

    final override val dataStorage = dataStorageFactory(File(File(".skyenet"), applicationName))
    protected open val appInfo = ServletHolder("appInfo", AppInfoServlet(applicationName))
    protected open val userInfo = ServletHolder("userInfo", UserInfoServlet())
    protected open val usageServlet = ServletHolder("usage", UsageServlet())
    protected open val fileZip = ServletHolder("fileZip", ZipServlet(dataStorage))
    protected open val fileIndex = ServletHolder("fileIndex", FileServlet(dataStorage))
    protected open val sessionSettingsServlet = ServletHolder("settings", SessionSettingsServlet(this))

    override fun newSession(userId: UserInfo?, sessionId: SessionID): SessionInterface {
        return object : ApplicationSession(
            sessionId = sessionId,
            userId = userId,
            dataStorage = dataStorage,
            applicationClass = this@ApplicationBase::class.java,
        ) {
            override fun processMessage(
                sessionId: SessionID,
                userId: UserInfo?,
                userMessage: String,
                session: ApplicationSession,
                sessionDiv: SessionDiv,
                socket: ChatSocket
            ) = this@ApplicationBase.processMessage(
                sessionId = sessionId,
                userId = userId,
                userMessage = userMessage,
                session = session,
                sessionDiv = sessionDiv,
                socket = socket
            )
        }
    }

    abstract fun processMessage(
        sessionId: SessionID,
        userId: UserInfo?,
        userMessage: String,
        session: ApplicationSession,
        sessionDiv: SessionDiv,
        socket: ChatSocket
    )

    open val settingsClass: Class<*> get() = Map::class.java

    open fun <T : Any> initSettings(sessionId: SessionID): T? = null

    fun <T : Any> getSettings(sessionId: SessionID, userId: UserInfo?): T? {
        @Suppress("UNCHECKED_CAST")
        var settings: T? = dataStorage.getJson(userId, sessionId, settingsClass as Class<T>, "settings.json")
        if (null == settings) {
            settings = initSettings(sessionId)
            if (null != settings) {
                dataStorage.setJson(userId, sessionId, settings, "settings.json")
            }
        }
        return settings
    }

    protected open fun sessionsServlet(path: String) = ServletHolder("sessionList", SessionListServlet(this.dataStorage, path))

    override fun configure(webAppContext: WebAppContext, path: String, baseUrl: String) {
        super.configure(webAppContext, path, baseUrl)

        webAppContext.addFilter(
            FilterHolder { request, response, chain ->
                val user = authenticationManager.getUser((request as HttpServletRequest).getCookie())
                val canRead = authorizationManager.isAuthorized(
                    applicationClass = this@ApplicationBase.javaClass,
                    user = user,
                    operationType = AuthorizationManager.OperationType.Read
                )
                if (canRead) {
                    chain?.doFilter(request, response)
                } else {
                    response?.writer?.write("Access Denied")
                    (response as HttpServletResponse?)?.status = HttpServletResponse.SC_FORBIDDEN
                }
            }, "/*", null
        )

        webAppContext.addServlet(appInfo, "/appInfo")
        webAppContext.addServlet(userInfo, "/userInfo")
        webAppContext.addServlet(usageServlet, "/usage")
        webAppContext.addServlet(fileIndex, "/fileIndex/*")
        webAppContext.addServlet(fileZip, "/fileZip")
        webAppContext.addServlet(sessionsServlet(path), "/sessions")
        webAppContext.addServlet(sessionSettingsServlet, "/settings")
    }

    companion object {
        private val log = LoggerFactory.getLogger(ApplicationBase::class.java)
        val spinner =
            """<div class="spinner-border" role="status"><span class="sr-only">Loading...</span></div>"""

        fun getMimeType(filename: String): String =
            when {
                filename.endsWith(".html") -> "text/html"
                filename.endsWith(".json") -> "application/json"
                filename.endsWith(".js") -> "application/json"
                filename.endsWith(".png") -> "image/png"
                filename.endsWith(".jpg") -> "image/jpeg"
                filename.endsWith(".jpeg") -> "image/jpeg"
                filename.endsWith(".gif") -> "image/gif"
                filename.endsWith(".svg") -> "image/svg+xml"
                filename.endsWith(".css") -> "text/css"
                else -> "text/plain"
            }
        fun HttpServletRequest.getCookie(name: String = COOKIE_NAME) = cookies?.find { it.name == name }?.value

    }

}

