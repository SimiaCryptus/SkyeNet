package com.simiacryptus.skyenet.servlet

import com.google.api.services.oauth2.model.Userinfo
import com.simiacryptus.openai.OpenAIClient
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.concurrent.atomic.AtomicInteger

class UsageServlet : HttpServlet() {
    public override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.contentType = "text/html"
        resp.status = HttpServletResponse.SC_OK

        val sessionId = req.getParameter("sessionId")
        if (null != sessionId) {
            serve(resp, getSessionUsageSummary(sessionId))
        } else {
            val userinfo = AuthenticatedWebsite.getUser(req)
            if (null == userinfo) {
                resp.status = HttpServletResponse.SC_BAD_REQUEST
            } else {
                val usage = getUserUsageSummary(userinfo)
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

    data class UsageCounters(
        val tokensPerModel: HashMap<OpenAIClient.Model, AtomicInteger> = HashMap(),
    )

    companion object {
        val log = org.slf4j.LoggerFactory.getLogger(UsageServlet::class.java)

        private val usagePerSession = HashMap<String, UsageCounters>()
        val sessionsByUser = HashMap<String, ArrayList<String>>()

        fun incrementUsage(sessionId: String, userinfo: Userinfo?, model: OpenAIClient.Model, tokens: Int) {
            val usage = usagePerSession.getOrPut(sessionId) {
                UsageCounters()
            }
            val tokensPerModel = usage.tokensPerModel.getOrPut(model) {
                AtomicInteger()
            }
            tokensPerModel.addAndGet(tokens)
            if (userinfo != null) {
                val sessions = sessionsByUser.getOrPut(userinfo.id) {
                    ArrayList()
                }
                sessions.add(sessionId)
            }
        }

        fun getUserUsageSummary(userinfo: Userinfo): Map<OpenAIClient.Model, Int> {
            val sessions = sessionsByUser[userinfo.id]
            return sessions?.flatMap { sessionId ->
                val usage = usagePerSession[sessionId]
                usage?.tokensPerModel?.entries?.map { (model, counter) ->
                    model to counter.get()
                } ?: emptyList()
            }?.groupBy { it.first }?.mapValues { it.value.map { it.second }.sum() } ?: emptyMap()
        }

        fun getSessionUsageSummary(sessionId: String): Map<OpenAIClient.Model, Int> {
            val usage = usagePerSession[sessionId]
            return usage?.tokensPerModel?.entries?.map { (model, counter) ->
                model to counter.get()
            }?.groupBy { it.first }?.mapValues { it.value.map { it.second }.sum() } ?: emptyMap()
        }

    }
}
