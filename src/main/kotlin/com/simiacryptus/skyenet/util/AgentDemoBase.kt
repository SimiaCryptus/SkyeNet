package com.simiacryptus.skyenet.util

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.Body
import com.simiacryptus.skyenet.Ears
import com.simiacryptus.skyenet.Head
import com.simiacryptus.skyenet.Heart
import com.simiacryptus.skyenet.heart.GroovyInterpreter
import com.simiacryptus.skyenet.webui.SkyenetSimpleSessionServer
import java.awt.Desktop
import java.io.File
import java.net.URI

abstract class AgentDemoBase {

    protected val apiKey = File(File(System.getProperty("user.home")),"openai.key").readText().trim()

    open fun heart(apiObjects: Map<String, Any>): Heart = GroovyInterpreter(apiObjects)

    abstract fun apiObjects(): Map<String, Any>

    fun runCommand(command: String) {
        val apiObjects = apiObjects()
        val body = Body(
            api = OpenAIClient(apiKey),
            apiObjects = apiObjects,
            heart = heart(apiObjects)
        )
        body.brain.model = "gpt-4-0314"
        body.run(command)
    }

    fun launchAgent() {
        val apiObjects = apiObjects() + mapOf(
            "sys" to SystemTools(),
        )
        val body = Body(
            api = OpenAIClient(apiKey),
            apiObjects = apiObjects,
            heart = heart(apiObjects)
        )
        // Launch the user interface
        val head = Head(body = body, ears = Ears(api = OpenAIClient(apiKey)))
        val jFrame = head.start(api = OpenAIClient(apiKey))
        // Wait for the window to close
        while (jFrame.isVisible) {
            Thread.sleep(100)
        }
    }

    fun launchWebAgent() {
        val port = 8080
        val agentDemoBase = this
        val server = object : SkyenetSimpleSessionServer(
            oauthConfig = File(File(System.getProperty("user.home")),"client_secret_google_oauth.json").absolutePath,
        ) {
            override fun apiObjects(): Map<String, Any> {
                return agentDemoBase.apiObjects()
            }

            override fun heart(apiObjects: Map<String, Any>): Heart {
                return agentDemoBase.heart(apiObjects)
            }
        }.start(port)
        Desktop.getDesktop().browse(URI("http://localhost:$port/"))
        server.join()
    }

}