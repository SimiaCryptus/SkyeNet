package com.simiacryptus.skyenet.servers

import com.simiacryptus.skyenet.actors.ParsedActor
import com.simiacryptus.skyenet.webui.MarkdownUtil.renderMarkdown
import com.simiacryptus.skyenet.webui.PersistentSessionBase
import com.simiacryptus.skyenet.webui.SessionDiv
import com.simiacryptus.skyenet.webui.MacroChat
import com.simiacryptus.util.JsonUtil
import org.slf4j.LoggerFactory

open class ParsedActorTestApp<T>(
    private val actor: ParsedActor<T>,
    applicationName: String = "ParsedActorTest_"+actor.parserClass.simpleName,
    temperature: Double = 0.3,
    oauthConfig: String? = null,
) : MacroChat(
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
        val response = actor.answer(userMessage, api = api)
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