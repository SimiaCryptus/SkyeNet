package com.simiacryptus.skyenet.servers

import com.simiacryptus.skyenet.actors.ParsedActor
import com.simiacryptus.skyenet.body.MarkdownUtil.renderMarkdown
import com.simiacryptus.skyenet.body.PersistentSessionBase
import com.simiacryptus.skyenet.body.SessionDiv
import com.simiacryptus.skyenet.body.SkyenetMacroChat
import com.simiacryptus.util.JsonUtil
import org.slf4j.LoggerFactory

open class ParsedActorTestApp<T>(
    private val actor: ParsedActor<T>,
    applicationName: String = "ParsedActorTest_"+actor.parserClass.simpleName,
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
            |${response.getText()}
            |```
            |${JsonUtil.toJson(response.getObj()!!)}
            |```
            """.trimMargin().trim())
        }</div>""", false)
    }

    companion object {
        val log = LoggerFactory.getLogger(ParsedActorTestApp::class.java)
    }

}