package com.simiacryptus.skyenet.webui.servlet

import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.ApplicationServices.authenticationManager
import com.simiacryptus.skyenet.core.platform.ApplicationServices.clientManager
import com.simiacryptus.skyenet.core.platform.ClientManager.RecordingThreadFactory
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.AuthorizationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationServer.Companion.getCookie
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class CancelThreadsServlet(
    private val server: ApplicationServer,
) : HttpServlet() {
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.contentType = "text/html"
        resp.status = HttpServletResponse.SC_OK
        if (req.parameterMap.containsKey("sessionId")) {
            val session = Session(req.getParameter("sessionId"))
            //language=HTML
            resp.writer.write(
                """
        |<html>
        |<head>
        |    <title>Cancel Session</title>
        |    <link rel="icon" type="image/svg+xml" href="/favicon.svg"/>
        |</head>
        |<body>
        |<form action="${req.contextPath}/cancel" method="post">
        |    <input type="hidden" name="sessionId" value="$session"/>
        |    CONFIRM: <input type='text' name="confirm" placeholder="Type 'confirm' to cancel" />
        |    <input type="submit" value="Cancel"/>
        |</form>
        |</body>
        |</html>
        """.trimMargin()
            )
        } else {
            resp.status = HttpServletResponse.SC_BAD_REQUEST
            resp.writer.write("Session ID is required")
        }
    }


    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        require(req.getParameter("confirm").lowercase() == "confirm") { "Confirmation text is required" }
        resp.contentType = "text/html"
        resp.status = HttpServletResponse.SC_OK
        if (!req.parameterMap.containsKey("sessionId")) {
            resp.status = HttpServletResponse.SC_BAD_REQUEST
            resp.writer.write("Session ID is required")
        } else {
            val session = Session(req.getParameter("sessionId"))
            val user = authenticationManager.getUser(req.getCookie())
            require(
                ApplicationServices.authorizationManager.isAuthorized(
                    javaClass,
                    user,
                    AuthorizationInterface.OperationType.Delete
                )
            )
            { "User $user is not authorized to cancel sessions" }
            if (session.isGlobal()) {
                require(
                    ApplicationServices.authorizationManager.isAuthorized(
                        javaClass,
                        user,
                        AuthorizationInterface.OperationType.Public
                    )
                )
                { "User $user is not authorized to cancel global sessions" }
            }
            val pool = clientManager.getPool(session, user)
            pool.shutdownNow()
            (pool.threadFactory as RecordingThreadFactory).threads.filter { it.isAlive }.forEach { it.interrupt() }
            resp.sendRedirect("/")
        }
    }

}