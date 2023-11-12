package com.simiacryptus.skyenet.webui

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.webui.AuthenticatedWebsite.Companion.getUser
import com.simiacryptus.util.JsonUtil
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.io.FileUtils
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.webapp.WebAppContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

abstract class SessionServerBase(
    override val applicationName: String,
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
            settings = initSettings<T>(sessionId)
            if (null != settings) {
                sessionDataStorage.updateSettings(sessionId, settings)
            }
        }
        return settings
    }

    fun <T : Any> updateSettings(sessionId: String, settings: T) {
        sessionDataStorage.updateSettings(sessionId, settings)
    }

    abstract val api: OpenAIClient

    open val sessionDataStorage = SessionDataStorage(File(File(".skynet"), applicationName))

    override fun configure(context: WebAppContext, prefix: String, baseURL: String) {
        super.configure(context, prefix, baseURL)

        if (null != oauthConfig) (AuthenticatedWebsite(
            "$baseURL/oauth2callback",
            this@SessionServerBase.applicationName
        ) {
            FileUtils.openInputStream(File(oauthConfig))
        }).configure(context)

        context.addServlet(appInfo, prefix + "appInfo")
        context.addServlet(userInfo, prefix + "userInfo")
        context.addServlet(fileIndex, prefix + "fileIndex/*")
        context.addServlet(fileZip, prefix + "fileZip")
        context.addServlet(sessionsServlet, prefix + "sessions")
        context.addServlet(settingsServlet, prefix + "settings")
    }

    protected open val fileZip = ServletHolder(
        "fileZip",
        object : HttpServlet() {
            override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
                val sessionID = req.getParameter("session")
                val path = req.parameterMap.get("path")?.find { it.isNotBlank() } ?: "/"
                val sessionDir = sessionDataStorage.getSessionDir(sessionID)
                val file = File(sessionDir, path)
                val zipFile = File.createTempFile("skynet", ".zip")
                try {
                    zipFile.deleteOnExit()
                    zipFile.outputStream().use { outputStream ->
                        val zip = ZipOutputStream(outputStream)
                        write(file, file, zip)
                        zip.close()
                    }
                    resp.contentType = "application/zip"
                    resp.status = HttpServletResponse.SC_OK
                    resp.outputStream.write(zipFile.readBytes())
                } finally {
                    zipFile.delete()
                }
            }

            private fun write(basePath: File, file: File, zip: ZipOutputStream) {
                if (file.isFile) {
                    val path = basePath.toURI().relativize(file.toURI()).path
                    zip.putNextEntry(ZipEntry(path))
                    zip.write(file.readBytes())
                    zip.closeEntry()
                } else {
                    file.listFiles()?.forEach {
                        write(basePath, it, zip)
                    }
                }
            }
        })

    class FileServelet(val sessionDataStorage: SessionDataStorage) : HttpServlet() {
        override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
            val path = req.pathInfo ?: "/"
            val pathSegments = path.split("/").filter { it.isNotBlank() }
            pathSegments.forEach {
                if (it == "..") throw IllegalArgumentException("Invalid path")
            }
            val sessionID = pathSegments.first()
            val sessionDir = sessionDataStorage.getSessionDir(sessionID)
            val filePath = pathSegments.drop(1).joinToString("/")
            val file = File(sessionDir, filePath)
            if (file.isFile) {
                val filename = file.name
                resp.contentType = Companion.getMimeType(filename)
                resp.status = HttpServletResponse.SC_OK
                file.inputStream().use { inputStream ->
                    resp.outputStream.use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } else {
                resp.contentType = "text/html"
                resp.status = HttpServletResponse.SC_OK
                val files = file.listFiles()?.filter { it.isFile }?.sortedBy { it.name }?.joinToString("<br/>") {
                    """<a href="${it.name}">${it.name}</a>"""
                } ?: ""
                val folders = file.listFiles()?.filter { !it.isFile }?.sortedBy { it.name }?.joinToString("<br/>") {
                    """<a href="${it.name}/">${it.name}</a>"""
                } ?: ""
                resp.writer.write(
                    """
                        |<html>
                        |<head>
                        |<title>Files</title>
                        |</head>
                        |<body>
                        |<h1>Archive</h1>
                        |<a href="${req.contextPath}/fileZip?session=$sessionID&path=$path">ZIP</a>
                        |<h1>Folders</h1>
                        |$folders
                        |<h1>Files</h1>
                        |$files
                        |</body>
                        |</html>
                        """.trimMargin()
                )
            }
        }
    }

    protected open val fileIndex = ServletHolder("fileIndex", FileServelet(sessionDataStorage))

    inner class SessionServlet : HttpServlet() {
        override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
            resp.contentType = "text/html"
            resp.status = HttpServletResponse.SC_OK
            val sessions = sessionDataStorage.listSessions()
            val links = sessions.joinToString("<br/>") { session ->
                val sessionName = sessionDataStorage.getSessionName(session)
                """<a href="javascript:void(0)" onclick="window.location.href='#$session';window.location.reload();">
                |$sessionName
                |</a><br/>""".trimMargin()
            }
            resp.writer.write(
                """
                                |<html>
                                |<head>
                                |<title>Sessions</title>
                                |</head>
                                |<body>
                                |$links
                                |</body>
                                |</html>
                                """.trimMargin()
            )
        }
    }

    protected open val sessionsServlet = ServletHolder(
        "sessionList",
        SessionServlet()
    )

    protected open val settingsServlet = ServletHolder("settings", SettingsServlet(this))

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

    class UserInfoServlet : HttpServlet() {
        public override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
            resp.contentType = "text/json"
            resp.status = HttpServletResponse.SC_OK
            val userinfo = getUser(req)
            if (null == userinfo) {
                resp.writer.write("{}")
            } else {
                resp.writer.write(JsonUtil.objectMapper().writeValueAsString(userinfo))
            }
        }
    }

    protected open val appInfo = ServletHolder(
        "appInfo",
        AppInfoServlet()
    )
    protected open val userInfo = ServletHolder(
        "userInfo",
        UserInfoServlet()
    )

    companion object {
        val log = org.slf4j.LoggerFactory.getLogger(SessionServerBase::class.java)
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

