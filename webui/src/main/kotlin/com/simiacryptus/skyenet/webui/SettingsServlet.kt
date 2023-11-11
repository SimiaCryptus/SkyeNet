package com.simiacryptus.skyenet.webui

import com.simiacryptus.util.JsonUtil
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.File

class SettingsServlet(
    val server: SessionServerBase,
) : HttpServlet() {
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.contentType = "text/html"
        resp.status = HttpServletResponse.SC_OK
        val sessionId = req.getParameter("sessionId")
        if (null == sessionId) {
            resp.status = HttpServletResponse.SC_BAD_REQUEST
            resp.writer.write("Session ID is required")
        } else {
            val settings = server.getSettings<Any>(sessionId)
            val json = if(settings != null) JsonUtil.toJson(settings) else ""
            resp.writer.write(
                """
                            |<html>
                            |<head>
                            |<title>Settings</title>
                            |</head>
                            |<body>
                            |<form action="${req.contextPath}/settings" method="post">
                            |<input type="hidden" name="sessionId" value="$sessionId" />
                            |<input type="hidden" name="action" value="save" />
                            |<textarea name="settings" style="width: 100%; height: 100px;">$json</textarea>
                            |<input type="submit" value="Save" />
                            |</form>
                            |</body>
                            |</html>
                            """.trimMargin()
            )
        }
    }

    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.contentType = "text/html"
        resp.status = HttpServletResponse.SC_OK
        val sessionId = req.getParameter("sessionId")
        if (null == sessionId) {
            resp.status = HttpServletResponse.SC_BAD_REQUEST
            resp.writer.write("Session ID is required")
        } else {
            val settings = JsonUtil.fromJson<Any>(req.getParameter("settings"), server.settingsClass)
            server.sessionDataStorage.updateSettings(sessionId, settings)
            // Redirect back to ${req.contextPath}/#<session>
            resp.sendRedirect("${req.contextPath}/#$sessionId")
        }
    }
}