package com.simiacryptus.skyenet.test

import com.simiacryptus.jopenai.API
import com.simiacryptus.skyenet.application.ApplicationServer
import com.simiacryptus.skyenet.actors.ParsedActor
import com.simiacryptus.skyenet.application.ApplicationInterface
import com.simiacryptus.skyenet.platform.Session
import com.simiacryptus.skyenet.platform.User
import com.simiacryptus.skyenet.session.SocketManagerBase
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import com.simiacryptus.jopenai.util.JsonUtil
import org.slf4j.LoggerFactory

open class ParsedActorTestApp<T : Any>(
    private val actor: ParsedActor<T>,
    applicationName: String = "ParsedActorTest_" + actor.parserClass.simpleName,
    temperature: Double = 0.3,
) : ApplicationServer(
    applicationName = applicationName,
    temperature = temperature,
) {
    override fun newSession(
        session: Session,
        user: User?,
        userMessage: String,
        ui: ApplicationInterface,
        api: API
    ) {
        val sessionMessage = ui.newMessage(SocketManagerBase.randomID(), spinner, false)
        sessionMessage.append("""<div>${renderMarkdown(userMessage)}</div>""", true)
        val response = actor.answer(userMessage, api = api)
        sessionMessage.append(
            """<div>${
                renderMarkdown(
                    """
            |${response.getText()}
            |```
            |${JsonUtil.toJson(response.getObj())}
            |```
            """.trimMargin().trim()
                )
            }</div>""", false
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(ParsedActorTestApp::class.java)
    }

}