package com.simiacryptus.skyenet.servlet

import com.simiacryptus.skyenet.webui.SessionDataStorage
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class SessionServlet(private val sessionDataStorage : SessionDataStorage) : HttpServlet() {
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.contentType = "text/html"
        resp.status = HttpServletResponse.SC_OK
        val sessions = sessionDataStorage.listSessions()
        val links = sessions.joinToString("<br/>") { session ->
            val sessionName = sessionDataStorage.getSessionName(session)
            """<a href="javascript:void(0)" onclick="window.location.href='#$session';window.location.reload();">
            |$sessionName
            |</a><br/>""".trimMargin()
        }
        resp.writer.write(
            """
                            |<html>
                            |<head>
                            |<title>Sessions</title>
                            |</head>
                            |<body>
                            |$links
                            |</body>
                            |</html>
                            """.trimMargin()
        )
    }
}