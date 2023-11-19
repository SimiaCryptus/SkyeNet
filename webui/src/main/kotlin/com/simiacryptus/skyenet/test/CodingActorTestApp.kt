package com.simiacryptus.skyenet.test

import com.simiacryptus.skyenet.ApplicationBase
import com.simiacryptus.skyenet.actors.CodingActor
import com.simiacryptus.skyenet.chat.ChatSocket
import com.simiacryptus.skyenet.platform.ApplicationServices
import com.simiacryptus.skyenet.session.*
import com.simiacryptus.skyenet.platform.AuthorizationManager
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import org.slf4j.LoggerFactory
import java.util.*

open class CodingActorTestApp(
    private val actor: CodingActor,
    applicationName: String = "CodingActorTest_" + actor.interpreter.javaClass.simpleName,
    temperature: Double = 0.3,
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
        val canPlay = ApplicationServices.authorizationManager.isAuthorized(
            this::class.java,
            socket.user?.email,
            AuthorizationManager.OperationType.Execute
        )
        val playLink = if(!canPlay) "" else {
            val htmlTools = session.htmlTools(sessionDiv.divID())
            """${
                htmlTools.hrefLink("href-link play-button") {
                    sessionDiv.append("""<div>Running...</div>""", true)
                    val result = response.run()
                    sessionDiv.append(
                        """
                                        |<pre>${result.resultValue}</pre>
                                        |<pre>${result.resultOutput}</pre>
                                        """.trimMargin(), false
                    )
                }
            }▶</a>"""
        }
        sessionDiv.append("""<div>${
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