package com.simiacryptus.skyenet.application

import com.simiacryptus.jopenai.API
import com.simiacryptus.skyenet.servlet.AppInfoServlet
import com.simiacryptus.skyenet.chat.ChatServer
import com.simiacryptus.skyenet.platform.ApplicationServices.authenticationManager
import com.simiacryptus.skyenet.platform.ApplicationServices.authorizationManager
import com.simiacryptus.skyenet.platform.ApplicationServices.dataStorageFactory
import com.simiacryptus.skyenet.servlet.*
import com.simiacryptus.skyenet.platform.AuthenticationManager.Companion.AUTH_COOKIE
import com.simiacryptus.skyenet.session.SocketManager
import com.simiacryptus.skyenet.platform.AuthorizationManager
import com.simiacryptus.skyenet.platform.DataStorage
import com.simiacryptus.skyenet.platform.Session
import com.simiacryptus.skyenet.platform.User
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.servlet.FilterHolder
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.webapp.WebAppContext
import org.slf4j.LoggerFactory
import java.io.File

abstract class ApplicationServer(
    final override val applicationName: String,
    resourceBase: String = "simpleSession",
    val temperature: Double = 0.1,
) : ChatServer(resourceBase) {

    final override val dataStorage: DataStorage = dataStorageFactory(File(File(".skyenet"), applicationName))
    protected open val appInfo = ServletHolder("appInfo", AppInfoServlet(applicationName))
    protected open val userInfo = ServletHolder("userInfo", UserInfoServlet())
    protected open val usageServlet = ServletHolder("usage", UsageServlet())
    protected open val fileZip = ServletHolder("fileZip", ZipServlet(dataStorage))
    protected open val fileIndex = ServletHolder("fileIndex", FileServlet(dataStorage))
    protected open val sessionSettingsServlet = ServletHolder("settings", SessionSettingsServlet(this))

    override fun newSession(user: User?, session: Session): SocketManager {
        return object : ApplicationSocketManager(
            session = session,
            user = user,
            dataStorage = dataStorage,
            applicationClass = this@ApplicationServer::class.java,
        ) {
            override fun newSession(
                session: Session,
                user: User?,
                userMessage: String,
                socketManager: ApplicationSocketManager,
                api: API
            ) = this@ApplicationServer.newSession(
                session = session,
                user = user,
                userMessage = userMessage,
                ui = socketManager.applicationInterface,
                api = api
            )
        }
    }

    abstract fun newSession(
        session: Session,
        user: User?,
        userMessage: String,
        ui: ApplicationInterface,
        api: API
    )

    open val settingsClass: Class<*> get() = Map::class.java

    open fun <T : Any> initSettings(session: Session): T? = null

    fun <T : Any> getSettings(session: Session, userId: User?): T? {
        @Suppress("UNCHECKED_CAST")
        var settings: T? = dataStorage.getJson(userId, session, "settings.json", settingsClass as Class<T>)
        if (null == settings) {
            settings = initSettings(session)
            if (null != settings) {
                dataStorage.setJson(userId, session, "settings.json", settings)
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
                    applicationClass = this@ApplicationServer.javaClass,
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
        private val log = LoggerFactory.getLogger(ApplicationServer::class.java)
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
        fun HttpServletRequest.getCookie(name: String = AUTH_COOKIE) = cookies?.find { it.name == name }?.value

    }

}

