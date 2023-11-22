package com.simiacryptus.skyenet.webui.servlet

import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.UserSettingsManager.UserSettings
import com.simiacryptus.skyenet.webui.application.ApplicationServer.Companion.getCookie
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class UserSettingsServlet : HttpServlet() {
    public override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.contentType = "text/html"
        resp.status = HttpServletResponse.SC_OK
        val userinfo = ApplicationServices.authenticationManager.getUser(req.getCookie())
        if (null == userinfo) {
            resp.status = HttpServletResponse.SC_BAD_REQUEST
        } else {
            val settings = ApplicationServices.userSettingsManager.getUserSettings(userinfo)
            val json = JsonUtil.toJson(settings)
            //language=HTML
            resp.writer.write(
                """
                |<html>
                |<head>
                |    <title>Settings</title>
                |    <link rel="icon" type="image/svg+xml" href="/favicon.svg"/>
                |</head>
                |<body>
                |<form action="/userSettings/" method="post">
                |    <input type="hidden" name="action" value="save"/>
                |    <textarea name="settings" style="width: 100%; height: 100px;">$json</textarea>
                |    <input type="submit" value="Save"/>
                |</form>
                |</body>
                |</html>
                """.trimMargin()
            )
        }
    }

    public override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        val userinfo = ApplicationServices.authenticationManager.getUser(req.getCookie())
        if (null == userinfo) {
            resp.status = HttpServletResponse.SC_BAD_REQUEST
        } else {
            val settings = JsonUtil.fromJson<UserSettings>(req.getParameter("settings"), UserSettings::class.java)
            ApplicationServices.userSettingsManager.updateUserSettings(userinfo, settings)
            resp.sendRedirect("/")
        }
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(UserSettingsServlet::class.java)
    }
}