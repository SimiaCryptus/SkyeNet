package com.simiacryptus.skyenet.servlet

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.ApplicationBase.Companion.getCookie
import com.simiacryptus.skyenet.config.ApplicationServices
import com.simiacryptus.skyenet.config.AuthenticationManager.Companion.COOKIE_NAME
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class UsageServlet : HttpServlet() {
    public override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.contentType = "text/html"
        resp.status = HttpServletResponse.SC_OK

        val sessionId = req.getParameter("sessionId")
        if (null != sessionId) {
            serve(resp, ApplicationServices.usageManager.getSessionUsageSummary(sessionId))
        } else {
            val userinfo = ApplicationServices.authenticationManager.getUser(getCookie(req, COOKIE_NAME))
            if (null == userinfo) {
                resp.status = HttpServletResponse.SC_BAD_REQUEST
            } else {
                val usage = ApplicationServices.usageManager.getUserUsageSummary(userinfo.id)
                serve(resp, usage)
            }
        }
    }

    private fun serve(
        resp: HttpServletResponse,
        usage: Map<OpenAIClient.Model, Int>
    ) {
        resp.writer.write(
            """
                    |<html>
                    |<head>
                    |    <title>Usage</title>
                    |    <link rel="icon" type="image/svg+xml" href="/favicon.svg"/>
                    |</head>
                    |<body>
                    |<table>
                    |    <tr>
                    |        <th>Model</th>
                    |        <th>Usage</th>
                    |    </tr>
                    |    ${
                usage.entries.joinToString("\n") { (model, count) ->
                    """
                        |<tr>
                        |    <td>$model</td>
                        |    <td>$count</td>
                        |</tr>
                        """.trimMargin()
                }
            }
                    |</table>
                    |</body>
                    |</html>
                    """.trimMargin()
        )
    }

    companion object {
        val log = org.slf4j.LoggerFactory.getLogger(UsageServlet::class.java)

    }
}

