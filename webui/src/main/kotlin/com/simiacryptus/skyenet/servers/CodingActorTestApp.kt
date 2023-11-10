package com.simiacryptus.skyenet.servers

import com.simiacryptus.skyenet.actors.CodingActor
import com.simiacryptus.skyenet.body.ChatSessionFlexmark.Companion.renderMarkdown
import com.simiacryptus.skyenet.body.PersistentSessionBase
import com.simiacryptus.skyenet.body.SessionDiv
import com.simiacryptus.skyenet.body.SkyenetMacroChat
import org.slf4j.LoggerFactory
import java.util.*

open class CodingActorTestApp(
    private val actor: CodingActor,
    applicationName: String = "CodingActorTest_"+actor.interpreter.javaClass.simpleName,
    temperature: Double = 0.3,
    oauthConfig: String? = null,
) : SkyenetMacroChat(
    applicationName = applicationName,
    oauthConfig = oauthConfig,
    temperature = temperature,
) {
    override fun processMessage(
        sessionId: String,
        userMessage: String,
        session: PersistentSessionBase,
        sessionUI: SessionUI,
        sessionDiv: SessionDiv
    ) {
        sessionDiv.append("""<div>${renderMarkdown(userMessage)}</div>""", true)
        val response = actor.answer(userMessage)
        sessionDiv.append("""<div>${
            renderMarkdown("""
            |```${actor.interpreter.getLanguage().lowercase(Locale.getDefault())}
            |${response.getCode()}
            |```
            |${sessionUI.hrefLink {
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
        val log = LoggerFactory.getLogger(CodingActorTestApp::class.java)
    }

}