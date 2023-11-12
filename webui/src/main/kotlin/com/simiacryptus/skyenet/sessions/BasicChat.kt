package com.simiacryptus.skyenet.sessions

import com.simiacryptus.openai.OpenAIClient

open class BasicChat(
    applicationName: String,
    oauthConfig: String? = null,
    val model: OpenAIClient.Model = OpenAIClient.Models.GPT35Turbo,
    val api: OpenAIClient,
) : ApplicationBase(
    applicationName = applicationName,
    oauthConfig = oauthConfig,
) {

    override fun newSession(sessionId: String): SessionInterface {
        val basicChatSession = BasicChatSession(
            parent = this@BasicChat,
            model = model,
            sessionId = sessionId,
            api = api
        )
        val handler = MutableSessionHandler(null)
        handler.setDelegate(basicChatSession)
        return handler
    }

}