package com.simiacryptus.skyenet.test

import com.simiacryptus.openai.OpenAIAPI
import com.simiacryptus.skyenet.ApplicationBase
import com.simiacryptus.skyenet.actors.SimpleActor
import com.simiacryptus.skyenet.platform.Session
import com.simiacryptus.skyenet.platform.User
import com.simiacryptus.skyenet.session.*
import com.simiacryptus.skyenet.util.MarkdownUtil
import org.slf4j.LoggerFactory

open class SimpleActorTestApp(
    private val actor: SimpleActor,
    applicationName: String = "SimpleActorTest_" + actor.javaClass.simpleName,
    temperature: Double = 0.3,
) : ApplicationBase(
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
        api: OpenAIAPI
    ) {
        val sessionMessage = ui.newMessage(SocketManagerBase.randomID(), ApplicationBase.spinner, false)
        val actor = getSettings<Settings>(session, user)?.actor ?: actor
        sessionMessage.append("""<div>${MarkdownUtil.renderMarkdown(userMessage)}</div>""", true)
        val moderatorResponse = actor.answer(userMessage, api = api)
        sessionMessage.append("""<div>${MarkdownUtil.renderMarkdown(moderatorResponse)}</div>""", false)
    }

    companion object {
        private val log = LoggerFactory.getLogger(SimpleActorTestApp::class.java)
    }

}