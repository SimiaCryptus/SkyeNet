package com.simiacryptus.skyenet

import com.simiacryptus.skyenet.chat.ChatServer
import com.simiacryptus.skyenet.chat.ChatSocket
import com.simiacryptus.skyenet.servlet.*
import com.simiacryptus.skyenet.session.SessionBase
import com.simiacryptus.skyenet.session.SessionDataStorage
import com.simiacryptus.skyenet.session.SessionDiv
import com.simiacryptus.skyenet.session.SessionInterface
import com.simiacryptus.skyenet.util.AuthorizationManager
import com.simiacryptus.skyenet.util.AuthorizationManager.isAuthorized
import com.simiacryptus.skyenet.util.HtmlTools
import com.simiacryptus.util.JsonUtil
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.io.FileUtils
import org.eclipse.jetty.servlet.FilterHolder
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.webapp.WebAppContext
import org.slf4j.LoggerFactory
import java.io.File

abstract class ApplicationBase(
    final override val applicationName: String,
    val oauthConfig: String? = null,
    resourceBase: String = "simpleSession",
    val temperature: Double = 0.1,
) : ChatServer(resourceBase) {


    class ApplicationSession(
        val parent: ApplicationBase,
        sessionId: String,
    ) : SessionBase(
        sessionId = sessionId,
        sessionDataStorage = parent.sessionDataStorage
    ) {
        private val threads = mutableMapOf<String, Thread>()

        val linkTriggers = mutableMapOf<String, java.util.function.Consumer<Unit>>()

        override fun onRun(userMessage: String, socket: ChatSocket) {
            val operationID = randomID()
            val sessionDiv = newSessionDiv(operationID, spinner, true)
            threads[operationID] = Thread.currentThread()
            parent.processMessage(sessionId, userMessage, this, sessionDiv, socket)
        }

        override fun onCmd(id: String, code: String, socket: ChatSocket) {
            if (code == "cancel") {
                threads[id]?.interrupt()
            }
            if (code == "link") {
                linkTriggers[id]?.accept(Unit)
            }
            super.onCmd(id, code, socket)
        }

        fun htmlTools(divID: String) = HtmlTools(this, divID)
    }

    override fun newSession(sessionId: String): SessionInterface {
        return ApplicationSession(this, sessionId)
    }

    abstract fun processMessage(
        sessionId: String,
        userMessage: String,
        session: ApplicationSession,
        sessionDiv: SessionDiv,
        socket: ChatSocket
    )

    open val settingsClass: Class<*> get() = Map::class.java

    open fun <T : Any> initSettings(sessionId: String): T? = null

    fun <T : Any> getSettings(sessionId: String): T? {
        @Suppress("UNCHECKED_CAST")
        var settings: T? = sessionDataStorage.getSettings(sessionId, settingsClass as Class<T>)
        if (null == settings) {
            settings = initSettings(sessionId)
            if (null != settings) {
                sessionDataStorage.updateSettings(sessionId, settings)
            }
        }
        return settings
    }

    fun <T : Any> updateSettings(sessionId: String, settings: T) {
        sessionDataStorage.updateSettings(sessionId, settings)
    }

    final override val sessionDataStorage = SessionDataStorage(File(File(".skyenet"), applicationName))


    override fun configure(webAppContext: WebAppContext, path: String, baseUrl: String) {
        super.configure(webAppContext, path, baseUrl)

        if (null != oauthConfig) AuthenticatedWebsite(
            "$baseUrl/oauth2callback",
            this@ApplicationBase.applicationName
        ) { FileUtils.openInputStream(File(oauthConfig)) }
            .configure(webAppContext)

        webAppContext.addFilter(
            FilterHolder { request, response, chain ->
                val user = AuthenticatedWebsite.getUser(request as HttpServletRequest)
                val canRead = isAuthorized(
                    applicationClass = this@ApplicationBase.javaClass,
                    user = user?.email,
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

        val fileZip = ServletHolder("fileZip", ZipServlet(sessionDataStorage))
        val fileIndex = ServletHolder("fileIndex", FileServlet(sessionDataStorage))
        val sessionsServlet = ServletHolder("sessionList", SessionServlet(this.sessionDataStorage, path))
        val sessionSettingsServlet = ServletHolder("settings", SessionSettingsServlet(this))

        webAppContext.addServlet(appInfo, "/appInfo")
        webAppContext.addServlet(userInfo, "/userInfo")
        webAppContext.addServlet(fileIndex, "/fileIndex/*")
        webAppContext.addServlet(fileZip, "/fileZip")
        webAppContext.addServlet(sessionsServlet, "/sessions")
        webAppContext.addServlet(sessionSettingsServlet, "/settings")
    }

    protected open val appInfo = ServletHolder("appInfo", AppInfoServlet())
    protected open val userInfo = ServletHolder("userInfo", UserInfoServlet())

    companion object {
        val log = LoggerFactory.getLogger(ApplicationBase::class.java)
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
    }

}

