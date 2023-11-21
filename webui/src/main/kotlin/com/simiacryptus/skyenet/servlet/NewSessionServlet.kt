package com.simiacryptus.skyenet.servlet

import com.simiacryptus.skyenet.platform.DataStorage
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class NewSessionServlet : HttpServlet() {
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        val sessionId = DataStorage.newGlobalID()
        resp.contentType = "text/plain"
        resp.status = HttpServletResponse.SC_OK
        resp.writer.write(sessionId.sessionId)
    }
}