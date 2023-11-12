package com.simiacryptus.skyenet.servers

import com.simiacryptus.skyenet.actors.CodingActor
import com.simiacryptus.skyenet.sessions.*
import com.simiacryptus.skyenet.util.HtmlTools
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import org.slf4j.LoggerFactory
import java.util.*

open class CodingActorTestApp(
    private val actor: CodingActor,
    applicationName: String = "CodingActorTest_" + actor.interpreter.javaClass.simpleName,
    temperature: Double = 0.3,
    oauthConfig: String? = null,
) : ChatApplicationBase(
    applicationName = applicationName,
    oauthConfig = oauthConfig,
    temperature = temperature,
) {

    override fun processMessage(
        sessionId: String,
        userMessage: String,
        session: PersistentSessionBase,
        sessionDiv: SessionDiv,
        socket: MessageWebSocket
    ) {
        sessionDiv.append("""<div>${renderMarkdown(userMessage)}</div>""", true)
        val response = actor.answer(userMessage, api = socket.api)
        sessionDiv.append("""<div>${
            renderMarkdown("""
            |```${actor.interpreter.getLanguage().lowercase(Locale.getDefault())}
            |${response.getCode()}
            |```
            |${
                sessionDiv.htmlTools.hrefLink {
                sessionDiv.append("""<div>Running...</div>""", true)
                val result = response.run()
                sessionDiv.append(
                    """
                    |<pre>${result.resultValue}</pre>
                    |<pre>${result.resultOutput}</pre>
                    """.trimMargin(), false
                )
            }}▶</a>
            |
            """.trimMargin().trim())
        }</div>""", false)
    }


    companion object {
        val SessionDiv.htmlTools get() = HtmlTools(divID())
        val log = LoggerFactory.getLogger(CodingActorTestApp::class.java)
    }

}