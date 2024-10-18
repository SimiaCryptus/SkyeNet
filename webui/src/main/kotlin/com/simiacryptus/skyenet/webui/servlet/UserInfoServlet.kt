package com.simiacryptus.skyenet.webui.servlet

import com.simiacryptus.util.JsonUtil
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.webui.application.ApplicationServer.Companion.getCookie
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class UserInfoServlet : HttpServlet() {
    public override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.contentType = "text/json"
        resp.status = HttpServletResponse.SC_OK
        val user: User? = ApplicationServices.authenticationManager.getUser(req.getCookie())
        if (null == user) {
            resp.writer.write("{}")
        } else {
            resp.writer.write(JsonUtil.objectMapper().writeValueAsString(user))
        }
    }
}