package com.simiacryptus.skyenet.servers

import com.simiacryptus.skyenet.actors.SimpleActor
import com.simiacryptus.skyenet.body.ChatSessionFlexmark
import com.simiacryptus.skyenet.body.PersistentSessionBase
import com.simiacryptus.skyenet.body.SessionDiv
import com.simiacryptus.skyenet.body.SkyenetMacroChat
import org.slf4j.LoggerFactory

open class SimpleActorTestApp(
    private val actor: SimpleActor,
    applicationName: String = "SimpleActorTest_"+actor.javaClass.simpleName,
    temperature: Double = 0.3,
    oauthConfig: String? = null,
) : SkyenetMacroChat(
    applicationName = applicationName,
    oauthConfig = oauthConfig,
    temperature = temperature,
) {

    override fun processMessage(
        sessionId: String,
        userMessage: String,
        session: PersistentSessionBase,
        sessionUI: SessionUI,
        sessionDiv: SessionDiv
    ) {
        sessionDiv.append("""<div>${ChatSessionFlexmark.renderMarkdown(userMessage)}</div>""", true)
        val moderatorResponse = actor.answer(userMessage)
        sessionDiv.append("""<div>${ChatSessionFlexmark.renderMarkdown(moderatorResponse)}</div>""", false)
    }

    companion object {
        val log = LoggerFactory.getLogger(SimpleActorTestApp::class.java)
    }

}