package com.simiacryptus.skyenet.test

import com.simiacryptus.skyenet.ApplicationBase
import com.simiacryptus.skyenet.ApplicationSession
import com.simiacryptus.skyenet.actors.SimpleActor
import com.simiacryptus.skyenet.chat.ChatSocket
import com.simiacryptus.skyenet.platform.SessionID
import com.simiacryptus.skyenet.platform.UserInfo
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
    @Suppress("UNCHECKED_CAST") override fun <T:Any> initSettings(sessionId: SessionID): T? = Settings(actor=actor) as T

    override fun processMessage(
        sessionId: SessionID,
        userId: UserInfo?,
        userMessage: String,
        session: ApplicationSession,
        sessionDiv: SessionDiv,
        socket: ChatSocket
    ) {
        val actor = getSettings<Settings>(sessionId, userId)?.actor ?: actor
        sessionDiv.append("""<div>${MarkdownUtil.renderMarkdown(userMessage)}</div>""", true)
        val moderatorResponse = actor.answer(userMessage, api = socket.api)
        sessionDiv.append("""<div>${MarkdownUtil.renderMarkdown(moderatorResponse)}</div>""", false)
    }

    companion object {
        private val log = LoggerFactory.getLogger(SimpleActorTestApp::class.java)
    }

}