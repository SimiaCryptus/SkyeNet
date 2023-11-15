package com.simiacryptus.skyenet.servlet

import com.simiacryptus.skyenet.ApplicationBase
import com.simiacryptus.skyenet.config.ApplicationServices
import com.simiacryptus.skyenet.config.AuthenticationManager
import com.simiacryptus.util.JsonUtil
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class SessionSettingsServlet(
    private val server: ApplicationBase,
) : HttpServlet() {
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.contentType = "text/html"
        resp.status = HttpServletResponse.SC_OK
        val sessionId = req.getParameter("sessionId")
        if (null != sessionId) {
            val settings = server.getSettings<Any>(sessionId, ApplicationServices.authenticationManager.getUser(
                req.cookies?.find { it.name == AuthenticationManager.COOKIE_NAME }?.value
            )?.id)
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
                    |    <input type="hidden" name="sessionId" value="$sessionId"/>
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
        val sessionId = req.getParameter("sessionId")
        if (null == sessionId) {
            resp.status = HttpServletResponse.SC_BAD_REQUEST
            resp.writer.write("Session ID is required")
        } else {
            val settings = JsonUtil.fromJson<Any>(req.getParameter("settings"), server.settingsClass)
            server.dataStorage.setJson(ApplicationServices.authenticationManager.getUser(
                req.cookies?.find { it.name == AuthenticationManager.COOKIE_NAME }?.value
            )?.id, sessionId, settings, "settings.json")
            resp.sendRedirect("${req.contextPath}/#$sessionId")
        }
    }
}