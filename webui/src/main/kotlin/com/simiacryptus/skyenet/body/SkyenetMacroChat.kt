package com.simiacryptus.skyenet.body

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.util.JsonUtil
import java.util.function.Consumer

abstract class SkyenetMacroChat(
    applicationName: String,
    oauthConfig: String? = null,
    temperature: Double = 0.1,
) : SkyenetSessionServerBase(
    applicationName = applicationName,
    oauthConfig = oauthConfig,
    temperature = temperature,
) {
    override val api: OpenAIClient
        get() = OpenAIClient()

    interface SessionUI {
        val spinner: String
        val playButton: String
        val cancelButton: String
        val regenButton: String
        fun hrefLink(handler: Consumer<Unit>): String
        fun textInput(handler: Consumer<String>): String
    }

    override fun newSession(sessionId: String): SessionInterface {
        val handler = MutableSessionHandler(null)
        handler.setDelegate(MacroChatSession(this,sessionId))
        return handler
    }

    abstract fun processMessage(
        sessionId: String,
        userMessage: String,
        session: PersistentSessionBase,
        sessionUI: SessionUI,
        sessionDiv: SessionDiv
    )

    companion object {
        val log = org.slf4j.LoggerFactory.getLogger(SkyenetMacroChat::class.java)
    }

}

abstract class SessionDiv {
    abstract fun append(htmlToAppend: String, showSpinner: Boolean) : Unit
    abstract fun sessionID(): String
    abstract fun divID(): String
}

