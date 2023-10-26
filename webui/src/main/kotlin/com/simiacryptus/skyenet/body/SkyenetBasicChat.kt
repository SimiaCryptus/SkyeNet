package com.simiacryptus.skyenet.body

import com.simiacryptus.openai.OpenAIClient

open class SkyenetBasicChat(
    applicationName: String,
    oauthConfig: String? = null,
    val model: OpenAIClient.Model = OpenAIClient.Models.GPT35Turbo,
) : SkyenetSessionServerBase(
    applicationName = applicationName,
    oauthConfig = oauthConfig,
) {
    override val api: OpenAIClient
        get() = OpenAIClient()

    override fun newSession(sessionId: String): SessionInterface {
        val handler = MutableSessionHandler(null)
        val basicChatSession = BasicChatSession(
            parent = this@SkyenetBasicChat,
            model = model,
            sessionId = sessionId
        )
        handler.setDelegate(basicChatSession)
        return handler
    }

}