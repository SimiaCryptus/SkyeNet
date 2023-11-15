package com.simiacryptus.skyenet.servlet

import com.simiacryptus.skyenet.ApplicationBase.Companion.getCookie
import com.simiacryptus.skyenet.config.ApplicationServices
import com.simiacryptus.skyenet.config.AuthenticationManager.Companion.COOKIE_NAME
import com.simiacryptus.skyenet.config.UserSettingsManager.UserSettings
import com.simiacryptus.util.JsonUtil
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class UserSettingsServlet : HttpServlet() {
    public override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.contentType = "text/html"
        resp.status = HttpServletResponse.SC_OK
        val userinfo = ApplicationServices.authenticationManager.getUser(getCookie(req, COOKIE_NAME))
        if (null == userinfo) {
            resp.status = HttpServletResponse.SC_BAD_REQUEST
        } else {
            val settings = ApplicationServices.userSettingsManager.getUserSettings(userinfo.id)
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
        val userinfo = ApplicationServices.authenticationManager.getUser(getCookie(req, COOKIE_NAME))
        if (null == userinfo) {
            resp.status = HttpServletResponse.SC_BAD_REQUEST
        } else {
            val settings = JsonUtil.fromJson<UserSettings>(req.getParameter("settings"), UserSettings::class.java)
            ApplicationServices.userSettingsManager.updateUserSettings(userinfo.id, settings)
            resp.sendRedirect("/")
        }
    }

    companion object {
        val log = org.slf4j.LoggerFactory.getLogger(UserSettingsServlet::class.java)
    }
}