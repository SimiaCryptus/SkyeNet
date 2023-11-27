package com.simiacryptus.skyenet.webui.servlet

import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.webui.application.ApplicationServer.Companion.getCookie
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class LogoutServlet : HttpServlet() {
    public override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        val cookie = req.getCookie()
        val user = ApplicationServices.authenticationManager.getUser(cookie)
        if (null == user) {
            resp.status = HttpServletResponse.SC_BAD_REQUEST
        } else {
            ApplicationServices.authenticationManager.logout(cookie ?: "", user)
            resp.sendRedirect("/")
        }
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(LogoutServlet::class.java)
    }
}