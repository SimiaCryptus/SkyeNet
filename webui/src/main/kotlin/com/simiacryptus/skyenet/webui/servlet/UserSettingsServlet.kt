package com.simiacryptus.skyenet.webui.servlet

import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.UserSettingsInterface.UserSettings
import com.simiacryptus.skyenet.webui.application.ApplicationServer.Companion.getCookie
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

private const val mask = "********"

class UserSettingsServlet : HttpServlet() {
    public override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.contentType = "text/html"
        resp.status = HttpServletResponse.SC_OK
        val userinfo = ApplicationServices.authenticationManager.getUser(req.getCookie())
        if (null == userinfo) {
            resp.status = HttpServletResponse.SC_BAD_REQUEST
        } else {
            val settings = ApplicationServices.userSettingsManager.getUserSettings(userinfo)
            val visibleSettings = settings.copy(
                apiKeys = settings.apiKeys.mapValues { when(it.value) {
                    "" -> ""
                    else -> mask
                } },
                apiBase = settings.apiBase.mapValues { when(it.value) {
                    null -> "https://api.openai.com/v1"
                    "" -> "https://api.openai.com/v1"
                    else -> settings.apiBase[it.key]!!
                } },
            )
            val json = JsonUtil.toJson(visibleSettings)
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
            val prevSettings = ApplicationServices.userSettingsManager.getUserSettings(userinfo)
            val reconstructedSettings = prevSettings.copy(
                apiKeys = settings.apiKeys.mapValues { when(it.value) {
                    "" -> ""
                    mask -> prevSettings.apiKeys[it.key]!!
                    else -> settings.apiKeys[it.key]!!
                } },
                apiBase = settings.apiBase.mapValues { when(it.value) {
                    null -> "https://api.openai.com/v1"
                    "" -> "https://api.openai.com/v1"
                    else -> settings.apiBase[it.key]!!
                } },
            )
            ApplicationServices.userSettingsManager.updateUserSettings(userinfo, reconstructedSettings)
            resp.sendRedirect("/")
        }
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(UserSettingsServlet::class.java)
    }
}