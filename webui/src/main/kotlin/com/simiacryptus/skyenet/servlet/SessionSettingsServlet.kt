package com.simiacryptus.skyenet.servlet

import com.simiacryptus.skyenet.application.ApplicationServer
import com.simiacryptus.skyenet.application.ApplicationServer.Companion.getCookie
import com.simiacryptus.skyenet.platform.ApplicationServices
import com.simiacryptus.skyenet.platform.Session
import com.simiacryptus.jopenai.util.JsonUtil
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class SessionSettingsServlet(
    private val server: ApplicationServer,
) : HttpServlet() {
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.contentType = "text/html"
        resp.status = HttpServletResponse.SC_OK
        if (req.parameterMap.containsKey("sessionId")) {
            val session = Session(req.getParameter("sessionId"))
            val settings = server.getSettings<Any>(session, ApplicationServices.authenticationManager.getUser(
                req.getCookie()
            ))
            val json = if(settings != null) JsonUtil.toJson(settings) else ""
            //language=HTML
            resp.writer.write(
                """
                    |<html>
                    |<head>
                    |    <title>Settings</title>
                    |    <link rel="icon" type="image/svg+xml" href="/favicon.svg"/>
                    |</head>
                    |<body>
                    |<form action="${req.contextPath}/settings" method="post">
                    |    <input type="hidden" name="sessionId" value="$session"/>
                    |    <input type="hidden" name="action" value="save"/>
                    |    <textarea name="settings" style="width: 100%; height: 100px;">$json</textarea>
                    |    <input type="submit" value="Save"/>
                    |</form>
                    |</body>
                    |</html>
                    """.trimMargin()
            )
        } else {
            resp.status = HttpServletResponse.SC_BAD_REQUEST
            resp.writer.write("Session ID is required")
        }
    }

    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.contentType = "text/html"
        resp.status = HttpServletResponse.SC_OK
        if (!req.parameterMap.containsKey("sessionId")) {
            resp.status = HttpServletResponse.SC_BAD_REQUEST
            resp.writer.write("Session ID is required")
        } else {
            val session = Session(req.getParameter("sessionId"))
            val settings = JsonUtil.fromJson<Any>(req.getParameter("settings"), server.settingsClass)
            server.dataStorage.setJson(
                ApplicationServices.authenticationManager.getUser(req.getCookie()),
                session, "settings.json", settings
            )
            resp.sendRedirect("${req.contextPath}/#$session")
        }
    }
}