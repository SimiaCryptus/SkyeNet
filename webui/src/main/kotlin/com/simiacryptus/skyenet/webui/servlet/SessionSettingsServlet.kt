package com.simiacryptus.skyenet.webui.servlet

import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.ApplicationServices.authenticationManager
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationServer.Companion.getCookie
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class SessionSettingsServlet(
    private val server: ApplicationServer,
) : HttpServlet() {
    val settingsClass = Map::class.java // server.settingsClass
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.contentType = "text/html"
        resp.status = HttpServletResponse.SC_OK
        if (req.parameterMap.containsKey("sessionId")) {
            val session = Session(req.getParameter("sessionId"))
            val user = authenticationManager.getUser(req.getCookie())
            val settings = server.getSettings(session, user, settingsClass)
            val json = if (settings != null) JsonUtil.toJson(settings) else ""
            if (req.parameterMap.containsKey("raw") && req.getParameter("raw") == "true") {
                resp.contentType = "application/json"
                resp.writer.write(json)
                return
            }
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
            if (!req.parameterMap.containsKey("sessionId")) {
                resp.status = HttpServletResponse.SC_BAD_REQUEST
                resp.writer.write("Session ID is required")
            } else {
                val session = Session(req.getParameter("sessionId"))
                val settings = JsonUtil.fromJson<Any>(req.getParameter("settings"), settingsClass)
                val user = authenticationManager.getUser(req.getCookie()) ?: "global"
                ApplicationServices.dataStorageRoot.resolve("$user/$session/settings.json")
                    .apply { parentFile.mkdirs() }
                    .writeText(JsonUtil.toJson(settings))
                resp.sendRedirect("${req.contextPath}/#$session")
            }
        }
    }
}