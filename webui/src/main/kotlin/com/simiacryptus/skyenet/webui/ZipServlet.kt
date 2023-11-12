package com.simiacryptus.skyenet.webui

import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipServlet(val sessionDataStorage: SessionDataStorage) : HttpServlet() {
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
}