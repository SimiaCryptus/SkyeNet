package com.simiacryptus.skyenet.servers

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.sessions.*
import org.slf4j.LoggerFactory

open class ReadOnlyApp(
    applicationName: String,
    temperature: Double = 0.3,
    oauthConfig: String? = null,
    val api: OpenAIClient,
) : ApplicationBase(
    applicationName = applicationName,
    oauthConfig = oauthConfig,
    temperature = temperature,
) {

    companion object {
        val log = LoggerFactory.getLogger(ReadOnlyApp::class.java)
    }

    override fun newSession(sessionId: String): SessionInterface {
        return BasicChatSession(
            parent = this@ReadOnlyApp,
            model = OpenAIClient.Models.GPT35Turbo,
            sessionId = sessionId,
            api = api
        )
    }


}