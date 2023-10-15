package com.simiacryptus.skyenet.body

import com.simiacryptus.openai.OpenAIClient
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

abstract class SkyenetSessionServerBase(
    override val applicationName: String,
    val oauthConfig: String? = null,
    resourceBase: String = "simpleSession",
    val temperature: Double = 0.1,
) : WebSocketServer(resourceBase) {

    abstract val api: OpenAIClient

    open val spinner =
        """<div class="spinner-border" role="status"><span class="sr-only">Loading...</span></div>"""

    open val sessionDataStorage = SessionDataStorage(File(File(".skynet"), applicationName))

    override fun configure(context: WebAppContext, prefix: String, baseURL: String) {
        super.configure(context, prefix, baseURL)

        if (null != oauthConfig) (AuthenticatedWebsite("$baseURL/oauth2callback", this@SkyenetSessionServerBase.applicationName) {
            FileUtils.openInputStream(File(oauthConfig))
        }).configure(context)

        context.addServlet(appInfo, prefix+"/appInfo")
        context.addServlet(fileIndex, prefix+"/fileIndex/*")
        context.addServlet(fileZip, prefix+"/fileZip")
        context.addServlet(sessionList, prefix+"/sessions")
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

    protected open val fileIndex = ServletHolder(
        "fileIndex",
        object : HttpServlet() {
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
                    resp.contentType = if (file.name.endsWith(".html")) {
                        "text/html"
                    } else if (file.name.endsWith(".json")) {
                        "application/json"
                    } else if (file.name.endsWith(".js")) {
                        "application/json"
                    } else if (file.name.endsWith(".png")) {
                        "image/png"
                    } else if (file.name.endsWith(".jpg")) {
                        "image/jpeg"
                    } else if (file.name.endsWith(".jpeg")) {
                        "image/jpeg"
                    } else if (file.name.endsWith(".gif")) {
                        "image/gif"
                    } else if (file.name.endsWith(".svg")) {
                        "image/svg+xml"
                    } else if (file.name.endsWith(".css")) {
                        "text/css"
                    } else {
                        "text/plain"
                    }
                    resp.status = HttpServletResponse.SC_OK
                    resp.writer.write(file.readText())
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
        })

    protected open val sessionList = ServletHolder(
        "sessionList",
        object : HttpServlet() {
            override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
                resp.contentType = "text/html"
                resp.status = HttpServletResponse.SC_OK
                val links = sessionDataStorage.listSessions().joinToString("<br/>") {
                    """<a href="javascript:void(0)" onclick="window.location.href='/#$it';window.location.reload();">
                                |${sessionDataStorage.getSessionName(it)}
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
        })


    protected open val appInfo = ServletHolder(
        "appInfo",
        object : HttpServlet() {
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
        })

    companion object {
        val logger = org.slf4j.LoggerFactory.getLogger(SkyenetSessionServerBase::class.java)
    }

}

