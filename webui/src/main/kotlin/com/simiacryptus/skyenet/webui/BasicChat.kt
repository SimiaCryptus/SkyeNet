package com.simiacryptus.skyenet.webui

import com.simiacryptus.openai.OpenAIClient

open class BasicChat(
    applicationName: String,
    oauthConfig: String? = null,
    val model: OpenAIClient.Model = OpenAIClient.Models.GPT35Turbo,
) : SessionServerBase(
    applicationName = applicationName,
    oauthConfig = oauthConfig,
) {
    override val api: OpenAIClient
        get() = OpenAIClient()

    override fun newSession(sessionId: String): SessionInterface {
        val basicChatSession = BasicChatSession(
            parent = this@BasicChat,
            model = model,
            sessionId = sessionId
        )
        val handler = MutableSessionHandler(null)
        handler.setDelegate(basicChatSession)
        return handler
    }

}