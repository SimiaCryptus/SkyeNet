package com.simiacryptus.skyenet.webui.test

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.skyenet.core.actors.ImageActor
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.util.MarkdownUtil.renderMarkdown
import org.slf4j.LoggerFactory

open class ImageActorTestApp(
    private val actor: ImageActor,
    applicationName: String = "ImageActorTest_" + actor.javaClass.simpleName,
    temperature: Double = 0.3,
) : ApplicationServer(
    applicationName = applicationName,
    path = "/imageActorTest",
) {

    data class Settings(
        val actor: ImageActor? = null,
    )

    override val settingsClass: Class<*> get() = Settings::class.java
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> initSettings(session: Session): T? = Settings(actor = actor) as T

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
            val actor = getSettings<Settings>(session, user)?.actor ?: actor
            message.echo(renderMarkdown(userMessage, ui = ui))
            val response = actor.answer(
                listOf(userMessage), api = api
            )
            message.verbose(response.text)
            message.image(response.image)
            message.complete()
        } catch (e: Throwable) {
            log.warn("Error flushing image", e)
            message.error(ui, e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ImageActorTestApp::class.java)
    }

}