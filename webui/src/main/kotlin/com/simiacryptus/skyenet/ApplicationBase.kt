package com.simiacryptus.skyenet

import com.simiacryptus.skyenet.chat.ChatServer
import com.simiacryptus.skyenet.chat.ChatSocket
import com.simiacryptus.skyenet.config.ApplicationServices.authenticationManager
import com.simiacryptus.skyenet.config.ApplicationServices.authorizationManager
import com.simiacryptus.skyenet.config.ApplicationServices.dataStorageFactory
import com.simiacryptus.skyenet.servlet.*
import com.simiacryptus.skyenet.config.AuthenticationManager.Companion.COOKIE_NAME
import com.simiacryptus.skyenet.session.SessionBase
import com.simiacryptus.skyenet.session.SessionDiv
import com.simiacryptus.skyenet.session.SessionInterface
import com.simiacryptus.skyenet.config.AuthorizationManager
import com.simiacryptus.skyenet.util.HtmlTools
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


    inner class ApplicationSession(
        sessionId: String,
        userId: String?,
    ) : SessionBase(
        sessionId = sessionId,
        dataStorage = dataStorage,
        userId = userId,
    ) {
        private val threads = mutableMapOf<String, Thread>()

        val linkTriggers = mutableMapOf<String, java.util.function.Consumer<Unit>>()

        override fun onRun(userMessage: String, socket: ChatSocket) {
            val operationID = randomID()
            val sessionDiv = newSessionDiv(operationID, spinner, true)
            threads[operationID] = Thread.currentThread()
            processMessage(sessionId, userMessage, this, sessionDiv, socket)
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

    override fun newSession(userId: String?, sessionId: String): SessionInterface {
        return ApplicationSession(sessionId, userId)
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

    fun <T : Any> getSettings(sessionId: String, userId: String?): T? {
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

    final override val dataStorage = dataStorageFactory(File(File(".skyenet"), applicationName))
    protected open val appInfo = ServletHolder("appInfo", AppInfoServlet())
    protected open val userInfo = ServletHolder("userInfo", UserInfoServlet())
    protected open val fileZip = ServletHolder("fileZip", ZipServlet(dataStorage))
    protected open val fileIndex = ServletHolder("fileIndex", FileServlet(dataStorage))
    protected open val sessionSettingsServlet = ServletHolder("settings", SessionSettingsServlet(this))
    protected open fun sessionsServlet(path: String) = ServletHolder("sessionList", SessionServlet(this.dataStorage, path))

    override fun configure(webAppContext: WebAppContext, path: String, baseUrl: String) {
        super.configure(webAppContext, path, baseUrl)

        webAppContext.addFilter(
            FilterHolder { request, response, chain ->
                val user = authenticationManager.getUser(getCookie(request as HttpServletRequest, COOKIE_NAME))
                val canRead = authorizationManager.isAuthorized(
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

        webAppContext.addServlet(appInfo, "/appInfo")
        webAppContext.addServlet(userInfo, "/userInfo")
        webAppContext.addServlet(fileIndex, "/fileIndex/*")
        webAppContext.addServlet(fileZip, "/fileZip")
        webAppContext.addServlet(sessionsServlet(path), "/sessions")
        webAppContext.addServlet(sessionSettingsServlet, "/settings")
    }


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

        fun getCookie(req: HttpServletRequest, name: String) = req.cookies?.find { it.name == name }?.value

    }

}

