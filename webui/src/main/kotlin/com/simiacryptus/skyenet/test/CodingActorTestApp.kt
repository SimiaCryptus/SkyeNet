package com.simiacryptus.skyenet.test

import com.simiacryptus.openai.OpenAIAPI
import com.simiacryptus.skyenet.application.ApplicationServer
import com.simiacryptus.skyenet.actors.CodingActor
import com.simiacryptus.skyenet.application.ApplicationInterface
import com.simiacryptus.skyenet.platform.*
import com.simiacryptus.skyenet.session.*
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import org.slf4j.LoggerFactory
import java.util.*

open class CodingActorTestApp(
    private val actor: CodingActor,
    applicationName: String = "CodingActorTest_" + actor.interpreter.javaClass.simpleName,
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
        api: OpenAIAPI
    ) {
        val sessionMessage = ui.newMessage(SocketManagerBase.randomID(), ApplicationServer.spinner, false)
        sessionMessage.append("""<div>${renderMarkdown(userMessage)}</div>""", true)
        val response = actor.answer(userMessage, api = api)
        val canPlay = ApplicationServices.authorizationManager.isAuthorized(
            this::class.java,
            user,
            AuthorizationManager.OperationType.Execute
        )
        val playLink = if(!canPlay) "" else {
            ui.hrefLink("▶", "href-link play-button") {
                sessionMessage.append("""<div>Running...</div>""", true)
                val result = response.run()
                sessionMessage.append(
                    """
                    |<pre>${result.resultValue}</pre>
                    |<pre>${result.resultOutput}</pre>
                    """.trimMargin(), false
                )
            }
        }
        sessionMessage.append("""<div>${
            renderMarkdown("""
            |```${actor.interpreter.getLanguage().lowercase(Locale.getDefault())}
            |${response.getCode()}
            |```
            |$playLink
            """.trimMargin().trim())
        }</div>""", false)
    }

    companion object {
        private val log = LoggerFactory.getLogger(CodingActorTestApp::class.java)
    }
}