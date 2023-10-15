package com.simiacryptus.skyenet.body

import com.simiacryptus.openai.OpenAIClient

open class SkyenetBasicChat(
    applicationName: String,
    oauthConfig: String? = null
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
            sessionId = sessionId
        )
        handler.setDelegate(basicChatSession)
        return handler
    }

}