package com.simiacryptus.skyenet.webui.test

import com.simiacryptus.jopenai.API
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import org.slf4j.LoggerFactory

open class SimpleActorTestApp(
    private val actor: SimpleActor,
    applicationName: String = "SimpleActorTest_" + actor.javaClass.simpleName,
    temperature: Double = 0.3,
) : ApplicationServer(
    applicationName = applicationName,
    temperature = temperature,
) {

    data class Settings(
        val actor: SimpleActor? = null,
    )
    override val settingsClass: Class<*> get() = Settings::class.java
    @Suppress("UNCHECKED_CAST") override fun <T:Any> initSettings(session: Session): T? = Settings(actor=actor) as T

    override fun newSession(
        session: Session,
        user: User?,
        userMessage: String,
        ui: ApplicationInterface,
        api: API
    ) {
        val message = ui.newTask()
        try {
            val actor = getSettings<Settings>(session, user)?.actor ?: actor
            message.echo(renderMarkdown(userMessage))
            val response = actor.answer(userMessage, api = api)
            message.complete(renderMarkdown(response))
        } catch (e: Throwable) {
            log.warn("Error", e)
            message.error(e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(SimpleActorTestApp::class.java)
    }

}