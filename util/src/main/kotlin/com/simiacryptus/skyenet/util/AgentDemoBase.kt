package com.simiacryptus.skyenet.util

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.Brain
import com.simiacryptus.skyenet.Heart
import com.simiacryptus.skyenet.body.SkyenetCodingSessionServer
import java.awt.Desktop
import java.io.File
import java.net.URI

abstract class AgentDemoBase {

    abstract fun heart(hands: java.util.Map<String, Object>): Heart

    abstract fun hands(): java.util.Map<String, Object>

    fun runCommand(command: String) {
        val hands = hands()
        val heart = heart(hands)
        val brain = Brain(
            api = OpenAIClient(OpenAIClient.keyTxt),
            symbols = hands,
            language = heart.getLanguage(),
        )
        brain.model = OpenAIClient.Models.GPT35Turbo
        val response = brain.implement(command)
        heart.run(response)
    }

    fun launchWebAgent() {
        val port = 8080
        val agentDemoBase = this
        val server = object : SkyenetCodingSessionServer(
            applicationName = "AgentDemo",
            oauthConfig = File(File(System.getProperty("user.home")),"client_secret_google_oauth.json").absolutePath,
            apiKey = OpenAIClient.keyTxt
        ) {
            override fun hands(): java.util.Map<String, Object> = agentDemoBase.hands()
            override fun heart(hands: java.util.Map<String, Object>): Heart = agentDemoBase.heart(hands)
        }.start(port)
        Desktop.getDesktop().browse(URI("http://localhost:$port/"))
        server.join()
    }

}