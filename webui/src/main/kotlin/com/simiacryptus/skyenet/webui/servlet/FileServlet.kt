package com.simiacryptus.skyenet.webui.servlet

import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationServer.Companion.getCookie
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.File

class FileServlet(val dataStorage: StorageInterface) : HttpServlet() {
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        val path = req.pathInfo ?: "/"
        val pathSegments = parsePath(path)
        val session = Session(pathSegments.first())
        val sessionDir = dataStorage.getSessionDir(ApplicationServices.authenticationManager.getUser(req.getCookie()), session)
        val filePath = pathSegments.drop(1).joinToString("/")
        val file = File(sessionDir, filePath)
        if (file.isFile) {
            val filename = file.name
            resp.contentType = ApplicationServer.getMimeType(filename)
            resp.status = HttpServletResponse.SC_OK
            file.inputStream().use { inputStream ->
                resp.outputStream.use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } else {
            if(req.pathInfo?.endsWith("/") == false) {
                resp.sendRedirect(req.requestURI + "/")
                return
            }
            resp.contentType = "text/html"
            resp.status = HttpServletResponse.SC_OK
            val files = file.listFiles()?.filter { it.isFile && !it.name.startsWith(".") }?.sortedBy { it.name }?.joinToString("<br/>\n") {
                """<a class="file-item" href="${it.name}">${it.name}</a>"""
            } ?: ""
            val folders = file.listFiles()?.filter { !it.isFile && !it.name.startsWith(".") }?.sortedBy { it.name }?.joinToString("<br/>\n") {
                """<a class="folder-item" href="${it.name}/">${it.name}</a>"""
            } ?: ""
            resp.writer.write(
                """
                    |<html>
                    |<head>
                    |<title>Files</title>
                    |<style>
                    |    body {
                    |        font-family: 'Arial', sans-serif;
                    |        background-color: #f4f4f4;
                    |        color: #333;
                    |        margin: 0;
                    |        padding: 20px;
                    |    }
                    |
                    |    .archive-title, .folders-title, .files-title {
                    |        font-size: 24px;
                    |        font-weight: bold;
                    |        margin-top: 0;
                    |    }
                    |
                    |    .zip-link {
                    |        color: #0056b3;
                    |        text-decoration: none;
                    |        font-size: 16px;
                    |        background-color: #e7f3ff;
                    |        padding: 10px 15px;
                    |        border-radius: 5px;
                    |        display: inline-block;
                    |        margin-top: 10px;
                    |    }
                    |
                    |    .zip-link:hover {
                    |        background-color: #d1e7ff;
                    |    }
                    |
                    |    .folders-container, .files-container {
                    |        background-color: white;
                    |        border: 1px solid #ddd;
                    |        padding: 15px;
                    |        border-radius: 5px;
                    |        margin-top: 20px;
                    |    }
                    |
                    |    .folder-item, .file-item {
                    |        color: #0056b3;
                    |        text-decoration: none;
                    |        display: block;
                    |        margin-bottom: 10px;
                    |        padding: 5px 0;
                    |    }
                    |
                    |    .folder-item:hover, .file-item:hover {
                    |        text-decoration: underline;
                    |    }
                    |
                    |    h1 {
                    |        border-bottom: 2px solid #ddd;
                    |        padding-bottom: 10px;
                    |    }
                    |</style>
                    |</head>
                    |<body>
                    |<h1 class="archive-title">Archive</h1>
                    |<a href="${req.contextPath}/fileZip?session=$session&path=$filePath" class="zip-link">ZIP</a>
                    |<h1 class="folders-title">Folders</h1>
                    |<div class="folders-container">
                    |$folders
                    |</div>
                    |<h1 class="files-title">Files</h1>
                    |<div class="files-container">
                    |$files
                    |</div>
                    |</body>
                    |</html>
                    """.trimMargin()
            )
        }
    }

    companion object {
        fun parsePath(path: String): List<String> {
            val pathSegments = path.split("/").filter { it.isNotBlank() }
            pathSegments.forEach {
                when {
                    it == ".." -> throw IllegalArgumentException("Invalid path")
                    it.any {
                        when {
                            it == ':' -> true
                            it == '/' -> true
                            it == '~' -> true
                            it == '\\' -> true
                            it.code < 32 -> true
                            it.code > 126 -> true
                            else -> false
                        }
                    } -> throw IllegalArgumentException("Invalid path")
                }
            }
            return pathSegments
        }
    }
}