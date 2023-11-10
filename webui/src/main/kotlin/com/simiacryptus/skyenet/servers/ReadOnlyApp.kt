package com.simiacryptus.skyenet.servers

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.body.*
import org.slf4j.LoggerFactory

open class ReadOnlyApp(
    applicationName: String,
    temperature: Double = 0.3,
    oauthConfig: String? = null,
) : SkyenetSessionServerBase(
    applicationName = applicationName,
    oauthConfig = oauthConfig,
    temperature = temperature,
) {

    companion object {
        val log = LoggerFactory.getLogger(ReadOnlyApp::class.java)
    }

    override val api: OpenAIClient
        get() = OpenAIClient()

    override fun newSession(sessionId: String): SessionInterface {
        return BasicChatSession(
            parent = this@ReadOnlyApp,
            model = OpenAIClient.Models.GPT35Turbo,
            sessionId = sessionId
        )
    }


}