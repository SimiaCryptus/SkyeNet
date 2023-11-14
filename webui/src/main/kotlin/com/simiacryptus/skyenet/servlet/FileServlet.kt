package com.simiacryptus.skyenet.servlet

import com.simiacryptus.skyenet.ApplicationBase
import com.simiacryptus.skyenet.session.SessionDataStorage
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.File

class FileServlet(val sessionDataStorage: SessionDataStorage) : HttpServlet() {
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
            resp.contentType = ApplicationBase.getMimeType(filename)
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