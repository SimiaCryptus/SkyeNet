package com.simiacryptus.skyenet.servlet

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.models.OpenAIModel
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
            val userinfo = ApplicationServices.authenticationManager.getUser(req.getCookie())
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
        usage: Map<OpenAIModel, OpenAIClient.Usage>
    ) {
        resp.writer.write(
            """
                    |<html>
                    |<head>
                    |    <title>Usage</title>
                    |    <link rel="icon" type="image/svg+xml" href="/favicon.svg"/>
                    |    <style>
                    |        body { font-family: Arial, sans-serif; }
                    |        table { width: 100%; border-collapse: collapse; }
                    |        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
                    |        tr:nth-child(even) { background-color: #f2f2f2; }
                    |    </style>
                    |</head>
                    |<body>
                    |<table class="usage-table">
                    |    <tr class="table-header">
                    |        <th>Model</th>
                    |        <th>Usage</th>
                    |    </tr>
                    |    ${
                usage.entries.joinToString("\n") { (model, count) ->
                    """
                        |<tr class="table-row">
                        |    <td class="model-cell">$model</td>
                        |    <td class="usage-cell">$count</td>
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

