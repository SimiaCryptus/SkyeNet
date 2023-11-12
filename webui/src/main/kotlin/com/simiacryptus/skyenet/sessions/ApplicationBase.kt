package com.simiacryptus.skyenet.sessions

import com.simiacryptus.skyenet.servlet.*
import com.simiacryptus.util.JsonUtil
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.io.FileUtils
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.webapp.WebAppContext
import org.slf4j.LoggerFactory
import java.io.File

abstract class ApplicationBase(
    final override val applicationName: String,
    val oauthConfig: String? = null,
    resourceBase: String = "simpleSession",
    val temperature: Double = 0.1,
) : WebSocketServer(resourceBase) {

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

    final override val sessionDataStorage = SessionDataStorage(File(File(".skynet"), applicationName))

    protected open val fileZip = ServletHolder("fileZip", ZipServlet(sessionDataStorage))
    protected open val fileIndex = ServletHolder("fileIndex", FileServlet(sessionDataStorage))
    protected open val sessionsServlet = ServletHolder("sessionList", SessionServlet(this.sessionDataStorage))
    protected open val sessionSettingsServlet = ServletHolder("settings", SessionSettingsServlet(this))

    override fun configure(webAppContext: WebAppContext, prefix: String, baseUrl: String) {
        super.configure(webAppContext, prefix, baseUrl)

        if (null != oauthConfig) (AuthenticatedWebsite(
            "$baseUrl/oauth2callback",
            this@ApplicationBase.applicationName
        ) {
            FileUtils.openInputStream(File(oauthConfig))
        }).configure(webAppContext)

        webAppContext.addServlet(appInfo, prefix + "appInfo")
        webAppContext.addServlet(userInfo, prefix + "userInfo")
        webAppContext.addServlet(fileIndex, prefix + "fileIndex/*")
        webAppContext.addServlet(fileZip, prefix + "fileZip")
        webAppContext.addServlet(sessionsServlet, prefix + "sessions")
        webAppContext.addServlet(sessionSettingsServlet, prefix + "settings")
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

