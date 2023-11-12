package com.simiacryptus.skyenet.servers

import com.simiacryptus.skyenet.actors.SimpleActor
import com.simiacryptus.skyenet.webui.*
import org.slf4j.LoggerFactory

open class SimpleActorTestApp(
    private val actor: SimpleActor,
    applicationName: String = "SimpleActorTest_" + actor.javaClass.simpleName,
    temperature: Double = 0.3,
    oauthConfig: String? = null,
) : MacroChat(
    applicationName = applicationName,
    oauthConfig = oauthConfig,
    temperature = temperature,
) {

    data class Settings(
        val actor: SimpleActor? = null,
    )
    override val settingsClass: Class<*> get() = Settings::class.java
    @Suppress("UNCHECKED_CAST") override fun <T:Any> initSettings(sessionId: String): T? = Settings(actor=actor) as T

    override fun processMessage(
        sessionId: String,
        userMessage: String,
        session: PersistentSessionBase,
        sessionUI: SessionUI,
        sessionDiv: SessionDiv,
        socket: MessageWebSocket
    ) {
        val actor = getSettings<Settings>(sessionId)?.actor ?: actor
        sessionDiv.append("""<div>${MarkdownUtil.renderMarkdown(userMessage)}</div>""", true)
        val moderatorResponse = actor.answer(userMessage, api = socket.api)
        sessionDiv.append("""<div>${MarkdownUtil.renderMarkdown(moderatorResponse)}</div>""", false)
    }

    companion object {
        val log = LoggerFactory.getLogger(SimpleActorTestApp::class.java)
    }

}