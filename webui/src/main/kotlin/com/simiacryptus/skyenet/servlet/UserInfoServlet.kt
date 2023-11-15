package com.simiacryptus.skyenet.servlet

import com.simiacryptus.skyenet.ApplicationBase.Companion.getCookie
import com.simiacryptus.skyenet.config.ApplicationServices
import com.simiacryptus.skyenet.config.AuthenticationManager.Companion.COOKIE_NAME
import com.simiacryptus.util.JsonUtil
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class UserInfoServlet : HttpServlet() {
    public override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.contentType = "text/json"
        resp.status = HttpServletResponse.SC_OK
        val userinfo = ApplicationServices.authenticationManager.getUser(getCookie(req, COOKIE_NAME))
        if (null == userinfo) {
            resp.writer.write("{}")
        } else {
            resp.writer.write(JsonUtil.objectMapper().writeValueAsString(userinfo))
        }
    }
}