package com.simiacryptus.skyenet.test

import com.simiacryptus.skyenet.ApplicationBase
import com.simiacryptus.skyenet.actors.ParsedActor
import com.simiacryptus.skyenet.chat.ChatSocket
import com.simiacryptus.skyenet.session.*
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import com.simiacryptus.util.JsonUtil
import org.slf4j.LoggerFactory

open class ParsedActorTestApp<T>(
    private val actor: ParsedActor<T>,
    applicationName: String = "ParsedActorTest_" + actor.parserClass.simpleName,
    temperature: Double = 0.3,
    oauthConfig: String? = null,
) : ApplicationBase(
    applicationName = applicationName,
    temperature = temperature,
) {
    override fun processMessage(
        sessionId: String,
        userMessage: String,
        session: ApplicationSession,
        sessionDiv: SessionDiv,
        socket: ChatSocket
    ) {
        sessionDiv.append("""<div>${renderMarkdown(userMessage)}</div>""", true)
        val response = actor.answer(userMessage, api = socket.api)
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