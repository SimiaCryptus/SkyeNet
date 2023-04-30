package com.simiacryptus.skyenet.util

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.Brain
import com.simiacryptus.skyenet.Heart
import com.simiacryptus.skyenet.body.SkyenetSessionServer
import java.awt.Desktop
import java.io.File
import java.net.URI

abstract class AgentDemoBase {

    protected val apiKey = File(File(System.getProperty("user.home")),"openai.key").readText().trim()

    abstract fun heart(hands: java.util.Map<String, Object>): Heart

    abstract fun hands(): java.util.Map<String, Object>

    fun runCommand(command: String) {
        val hands = hands()
        val heart = heart(hands)
        val brain = Brain(
            api = OpenAIClient(apiKey),
            hands = hands,
            language = heart.getLanguage(),
        )
        brain.model = "gpt-4-0314"
        heart.run(brain.implement(command))
    }

    fun launchWebAgent() {
        val port = 8080
        val agentDemoBase = this
        val server = object : SkyenetSessionServer(
            applicationName = "AgentDemo",
            oauthConfig = File(File(System.getProperty("user.home")),"client_secret_google_oauth.json").absolutePath,
        ) {
            override fun hands(): java.util.Map<String, Object> = agentDemoBase.hands()
            override fun heart(hands: java.util.Map<String, Object>): Heart = agentDemoBase.heart(hands)
        }.start(port)
        Desktop.getDesktop().browse(URI("http://localhost:$port/"))
        server.join()
    }

}