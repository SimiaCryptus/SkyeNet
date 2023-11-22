package com.simiacryptus.skyenet.webui.servlet

import com.simiacryptus.jopenai.util.JsonUtil
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class AppInfoServlet(val applicationName:String) : HttpServlet() {
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.contentType = "text/json"
        resp.status = HttpServletResponse.SC_OK
        resp.writer.write(
            JsonUtil.objectMapper().writeValueAsString(
                mapOf(
                    "applicationName" to applicationName
                )
            )
        )
    }
}