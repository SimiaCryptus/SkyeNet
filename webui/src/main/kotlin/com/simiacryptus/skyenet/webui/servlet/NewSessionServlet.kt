package com.simiacryptus.skyenet.webui.servlet

import com.simiacryptus.skyenet.core.platform.Session
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class NewSessionServlet : HttpServlet() {
  override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
    val sessionId = Session.newGlobalID()
    resp.contentType = "text/plain"
    resp.status = HttpServletResponse.SC_OK
    resp.writer.write(sessionId.toString())
  }
}