package com.simiacryptus.skyenet.servlet

import com.simiacryptus.skyenet.config.ApplicationServices
import com.simiacryptus.skyenet.config.AuthenticationManager
import com.simiacryptus.skyenet.config.DataStorage
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class SessionServlet(
    private val dataStorage: DataStorage,
    private val prefix: String
) : HttpServlet() {
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.contentType = "text/html"
        resp.status = HttpServletResponse.SC_OK
        val sessions = dataStorage.listSessions(ApplicationServices.authenticationManager.getUser(
            req.cookies?.find { it.name == AuthenticationManager.COOKIE_NAME }?.value
        )?.id)
        // onclick="window.location.href='#$session';window.location.reload();"
        val links = sessions.joinToString("<br/>") { session ->
            val sessionName = dataStorage.getSessionName(
                ApplicationServices.authenticationManager.getUser(
                req.cookies?.find { it.name == AuthenticationManager.COOKIE_NAME }?.value
            )?.id, session)
            """<a href="$prefix#$session">
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