package com.simiacryptus.skyenet.webui.test

import com.simiacryptus.jopenai.API
import com.simiacryptus.skyenet.core.actors.CodingActor
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.AuthorizationManager
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.session.SocketManagerBase
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
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
        api: API
    ) {
        val sessionMessage = ui.newMessage(SocketManagerBase.randomID(), spinner, false)
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