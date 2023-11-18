package com.simiacryptus.skyenet.servlet

import com.simiacryptus.skyenet.ApplicationBase.Companion.getCookie
import com.simiacryptus.skyenet.config.ApplicationServices
import com.simiacryptus.skyenet.config.AuthenticationManager
import com.simiacryptus.skyenet.config.DataStorage
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class SessionListServlet(
    private val dataStorage: DataStorage,
    private val prefix: String
) : HttpServlet() {
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.contentType = "text/html"
        resp.status = HttpServletResponse.SC_OK
        val sessions = dataStorage.listSessions(ApplicationServices.authenticationManager.getUser(req.getCookie())?.id)

        resp.writer.write(
            """
            <html>
            <head>
            <title>Sessions</title>
            <style>
                body { font-family: Arial, sans-serif; }
                table { width: 100%; border-collapse: collapse; }
                th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
                th { background-color: #f2f2f2; }
                tr:hover { background-color: #ddd; }
                a { text-decoration: none; color: #333; }
            </style>
            </head>
            <body>
            <table>
                <tr>
                    <th>Session Name</th>
                </tr>
                ${sessions.joinToString("") { session ->
                    """
                    <tr class='session-row'>
                        <td><a href="$prefix#$session" class='session-link'>${sessionName(req, session)}</a></td>
                    </tr>
                    """.trimIndent()
                }}
            </table>
            </body>
            </html>
            """.trimIndent()
        )
    }

    private fun sessionName(req: HttpServletRequest, session: String) = dataStorage.getSessionName(
        ApplicationServices.authenticationManager.getUser(req.getCookie())?.id, session
    )
}