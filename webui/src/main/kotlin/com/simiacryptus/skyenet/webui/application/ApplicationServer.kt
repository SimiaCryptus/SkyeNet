package com.simiacryptus.skyenet.webui.application

import com.simiacryptus.jopenai.API
import com.simiacryptus.skyenet.core.platform.ApplicationServices.authenticationManager
import com.simiacryptus.skyenet.core.platform.ApplicationServices.authorizationManager
import com.simiacryptus.skyenet.core.platform.ApplicationServices.dataStorageFactory
import com.simiacryptus.skyenet.core.platform.AuthenticationInterface
import com.simiacryptus.skyenet.core.platform.AuthorizationInterface.OperationType
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.chat.ChatServer
import com.simiacryptus.skyenet.webui.servlet.*
import com.simiacryptus.skyenet.webui.session.SocketManager
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.servlet.FilterHolder
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.webapp.WebAppContext
import org.slf4j.LoggerFactory
import java.io.File

abstract class ApplicationServer(
    final override val applicationName: String,
    resourceBase: String = "application",
    open val root: File = File(File(".skyenet"), applicationName),
) : ChatServer(resourceBase) {

    open val description: String = ""
    open val singleInput = true
    open val appInfo: Any by lazy {
        mapOf(
            "applicationName" to applicationName,
            "singleInput" to singleInput
        )
    }

    final override val dataStorage: StorageInterface by lazy { dataStorageFactory(root) }

    protected open val appInfoServlet by lazy { ServletHolder("appInfo", AppInfoServlet(appInfo)) }
    protected open val userInfo by lazy { ServletHolder("userInfo", UserInfoServlet()) }
    protected open val usageServlet by lazy { ServletHolder("usage", UsageServlet()) }
    protected open val fileZip by lazy { ServletHolder("fileZip", ZipServlet(dataStorage)) }
    protected open val fileIndex by lazy { ServletHolder("fileIndex", FileServlet(dataStorage)) }
    protected open val sessionSettingsServlet by lazy { ServletHolder("settings", SessionSettingsServlet(this)) }
    protected open val sessionThreadsServlet by lazy { ServletHolder("threads", SessionThreadsServlet(this)) }
    protected open val deleteSessionServlet by lazy { ServletHolder("delete", DeleteSessionServlet(this)) }
    protected open val cancelSessionServlet by lazy { ServletHolder("cancel", DeleteSessionServlet(this)) }

    override fun newSession(user: User?, session: Session): SocketManager =
        object : ApplicationSocketManager(
            session = session,
            owner = user,
            dataStorage = dataStorage,
            applicationClass = this@ApplicationServer::class.java,
        ) {
            override fun userMessage(
                session: Session,
                user: User?,
                userMessage: String,
                socketManager: ApplicationSocketManager,
                api: API
            ) = this@ApplicationServer.userMessage(
                session = session,
                user = user,
                userMessage = userMessage,
                ui = socketManager.applicationInterface,
                api = api
            )
        }

    open fun userMessage(
        session: Session,
        user: User?,
        userMessage: String,
        socketManager: ApplicationSocketManager,
        api: API
    ) : Unit = this@ApplicationServer.userMessage(
        session = session,
        user = user,
        userMessage = userMessage,
        ui = socketManager.applicationInterface,
        api = api
    )
    open fun userMessage(
        session: Session,
        user: User?,
        userMessage: String,
        ui: ApplicationInterface,
        api: API
    ) : Unit = throw UnsupportedOperationException()

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

    protected open fun sessionsServlet(path: String) = ServletHolder("sessionList", SessionListServlet(this.dataStorage, path, this))

    override fun configure(webAppContext: WebAppContext, path: String, baseUrl: String) {
        super.configure(webAppContext, path, baseUrl)

        webAppContext.addFilter(
            FilterHolder { request, response, chain ->
                val user = authenticationManager.getUser((request as HttpServletRequest).getCookie())
                val canRead = authorizationManager.isAuthorized(
                    applicationClass = this@ApplicationServer.javaClass,
                    user = user,
                    operationType = OperationType.Read
                )
                if (canRead) {
                    chain?.doFilter(request, response)
                } else {
                    response?.writer?.write("Access Denied")
                    (response as HttpServletResponse?)?.status = HttpServletResponse.SC_FORBIDDEN
                }
            }, "/*", null
        )

        webAppContext.addServlet(appInfoServlet, "/appInfo")
        webAppContext.addServlet(userInfo, "/userInfo")
        webAppContext.addServlet(usageServlet, "/usage")
        webAppContext.addServlet(fileIndex, "/fileIndex/*")
        webAppContext.addServlet(fileZip, "/fileZip")
        webAppContext.addServlet(sessionsServlet(path), "/sessions")
        webAppContext.addServlet(sessionSettingsServlet, "/settings")
        webAppContext.addServlet(sessionThreadsServlet, "/threads")
        webAppContext.addServlet(deleteSessionServlet, "/delete")
        webAppContext.addServlet(cancelSessionServlet, "/cancel")
    }

    companion object {
        private val log = LoggerFactory.getLogger(ApplicationServer::class.java)

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
        fun HttpServletRequest.getCookie(name: String = AuthenticationInterface.AUTH_COOKIE) =
            cookies?.find { it.name == name }?.value

    }

}

