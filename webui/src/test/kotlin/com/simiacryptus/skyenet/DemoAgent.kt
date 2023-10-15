@file:Suppress("unused")

package com.simiacryptus.skyenet

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.body.SessionServerUtil.asJava
import com.simiacryptus.skyenet.body.SkyenetCodingSessionServer
import com.simiacryptus.skyenet.heart.ScalaLocalInterpreter
import com.simiacryptus.util.describe.AbbrevWhitelistYamlDescriber

import java.awt.Desktop
import java.net.URI

object DemoAgent {

    class Tools {
        fun client() = org.apache.http.impl.client.HttpClients.createDefault()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val port = 8081
        val baseURL = "http://localhost:$port"
        val typeDescriber = AbbrevWhitelistYamlDescriber(
            "com.simiacryptus",
        )
        val apiKey = OpenAIClient.keyTxt
        val server = object : SkyenetCodingSessionServer(
            applicationName = "Skyenet Agent Demo",
            //oauthConfig = File(File(System.getProperty("user.home")), "client_secret_google_oauth.json").absolutePath,
            typeDescriber = typeDescriber,
            model = OpenAIClient.Models.GPT35Turbo,
            apiKey = apiKey
        ) {
            override fun hands() = mapOf(
                "tools" to Tools() as Object,
            ).asJava

//            override fun heart(hands: java.util.Map<String, Object>): Heart = GroovyInterpreter(hands)
//            override fun heart(hands: java.util.Map<String, Object>): Heart = KotlinLocalInterpreter(hands)
            override fun heart(hands: java.util.Map<String, Object>): Heart = ScalaLocalInterpreter::class.java.getConstructor(java.util.Map::class.java).newInstance(hands)

        }.start(port)
        Desktop.getDesktop().browse(URI(baseURL))
        server.join()
    }
}