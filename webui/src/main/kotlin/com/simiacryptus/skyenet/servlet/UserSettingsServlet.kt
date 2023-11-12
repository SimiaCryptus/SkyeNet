package com.simiacryptus.skyenet.servlet

import com.google.api.services.oauth2.model.Userinfo
import com.simiacryptus.util.JsonUtil
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class UserSettingsServlet : HttpServlet() {
    public override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.contentType = "text/html"
        resp.status = HttpServletResponse.SC_OK
        val authId = req.cookies?.firstOrNull { it.name == "sessionId" }?.value
        if (null == authId) {
            resp.status = HttpServletResponse.SC_BAD_REQUEST
        } else {
            val userinfo = AuthenticatedWebsite.users[authId]
            if(null == userinfo) {
                resp.status = HttpServletResponse.SC_BAD_REQUEST
            } else {
                val settings = getUserSettings(userinfo)
                val json = if (settings != null) JsonUtil.toJson(settings) else ""
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
                    |    <input type="hidden" name="sessionId" value="$authId"/>
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
    }

    public override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        val userinfo = AuthenticatedWebsite.getUser(req)
        if (null == userinfo) {
            resp.status = HttpServletResponse.SC_BAD_REQUEST
        } else {
            val settings = JsonUtil.fromJson<UserSettings>(req.getParameter("settings"), UserSettings::class.java)
            updateUserSettings(userinfo, settings)
            resp.sendRedirect("/")
        }
    }

    companion object {
        val log = org.slf4j.LoggerFactory.getLogger(UserSettingsServlet::class.java)
        private val userSettings = HashMap<String, UserSettings>()

        fun getUserSettings(userinfo: Userinfo): UserSettings {
            return userSettings.getOrPut(userinfo.id) {
                UserSettings()
            }
        }

        fun updateUserSettings(userinfo: Userinfo, settings: UserSettings) {
            userSettings[userinfo.id] = settings
        }

    }
}
data class UserSettings(
    val apiKey: String = "",
)
