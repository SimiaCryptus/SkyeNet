package com.simiacryptus.skyenet.webui.test

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.skyenet.core.actors.CodingActor
import com.simiacryptus.skyenet.core.actors.CodingActor.Companion.indent
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.AuthorizationInterface.OperationType
import com.simiacryptus.skyenet.core.platform.ClientManager
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import org.slf4j.LoggerFactory
import java.util.*

open class CodingActorTestApp(
    private val actor: CodingActor,
    applicationName: String = "CodingActorTest_" + actor.name,
    temperature: Double = 0.3,
) : ApplicationServer(
    applicationName = applicationName,
    path = "/codingActorTest",
) {
    override fun userMessage(
        session: Session,
        user: User?,
        userMessage: String,
        ui: ApplicationInterface,
        api: API
    ) {
        (api as ChatClient).budget = 2.00
        val message = ui.newTask()
        try {
            message.echo(renderMarkdown(userMessage, ui = ui))
            val response = actor.answer(CodingActor.CodeRequest(listOf(userMessage to ApiModel.Role.user)), api = api)
            val canPlay = ApplicationServices.authorizationManager.isAuthorized(
                this::class.java,
                user,
                OperationType.Execute
            )
            val playLink = if (!canPlay) "" else {
                ui.hrefLink("▶", "href-link play-button") {
                    message.add("Running...")
                    val result = response.result
                    message.complete(
                        """
                                        |<pre>${result.resultValue}</pre>
                                        |<pre>${result.resultOutput}</pre>
                                        """.trimMargin()
                    )
                }
            }
            message.complete(
                renderMarkdown(
                    """
                    |```${actor.language.lowercase(Locale.getDefault())}
                    |${/*escapeHtml4*/(response.code)/*.indent("  ")*/}
                    |```
                    |$playLink
                    """.trimMargin().trim(), ui = ui
                )
            )
        } catch (e: Throwable) {
            log.warn("Error", e)
            message.error(ui, e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(CodingActorTestApp::class.java)
    }

}