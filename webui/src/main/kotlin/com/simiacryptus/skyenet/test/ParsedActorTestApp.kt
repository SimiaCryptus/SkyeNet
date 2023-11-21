package com.simiacryptus.skyenet.test

import com.simiacryptus.skyenet.ApplicationBase
import com.simiacryptus.skyenet.session.ApplicationSocketManager
import com.simiacryptus.skyenet.actors.ParsedActor
import com.simiacryptus.skyenet.chat.ChatSocket
import com.simiacryptus.skyenet.platform.Session
import com.simiacryptus.skyenet.platform.User
import com.simiacryptus.skyenet.session.SessionMessage
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import com.simiacryptus.util.JsonUtil
import org.slf4j.LoggerFactory

open class ParsedActorTestApp<T : Any>(
    private val actor: ParsedActor<T>,
    applicationName: String = "ParsedActorTest_" + actor.parserClass.simpleName,
    temperature: Double = 0.3,
) : ApplicationBase(
    applicationName = applicationName,
    temperature = temperature,
) {
    override fun newSession(
        session: Session,
        user: User?,
        userMessage: String,
        socketManager: ApplicationSocketManager.ApplicationInterface,
        sessionMessage: SessionMessage,
        socket: ChatSocket
    ) {
        sessionMessage.append("""<div>${renderMarkdown(userMessage)}</div>""", true)
        val response = actor.answer(userMessage, api = socket.api)
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