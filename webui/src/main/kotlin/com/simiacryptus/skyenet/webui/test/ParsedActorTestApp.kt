package com.simiacryptus.skyenet.webui.test

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.core.actors.ParsedActor
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
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
        val message = ui.newTask()
        try {
            message.echo(renderMarkdown(userMessage))
            val response = actor.answer(listOf(userMessage), api = api)
            message.complete(
                renderMarkdown(
                    """
                    |${response.getText()}
                    |```
                    |${JsonUtil.toJson(response.getObj())}
                    |```
                    """.trimMargin().trim()
                    )
            )
        } catch (e: Throwable) {
            log.warn("Error", e)
            message.error(e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ParsedActorTestApp::class.java)
    }

}