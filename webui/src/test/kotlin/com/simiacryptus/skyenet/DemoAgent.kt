@file:Suppress("unused")

package com.simiacryptus.skyenet

import com.simiacryptus.skyenet.heart.GroovyInterpreter
import com.simiacryptus.skyenet.body.SessionServerUtil.asJava
import com.simiacryptus.skyenet.body.SkyenetCodingSessionServer
import com.simiacryptus.util.AbbrevWhitelistYamlDescriber

import java.awt.Desktop
import java.io.File
import java.net.URI

object DemoAgent {

    class Tools {
        fun client() = org.apache.http.impl.client.HttpClients.createDefault()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val port = 8081
        val baseURL = "http://localhost:$port"
        val server = object : SkyenetCodingSessionServer(
            applicationName = "Skyenet Agent Demo",
            //oauthConfig = File(File(System.getProperty("user.home")), "client_secret_google_oauth.json").absolutePath,
            yamlDescriber = AbbrevWhitelistYamlDescriber(
                "com.simiacryptus",
            ),
            baseURL = baseURL,
            model = "gpt-4-0314",
            apiKey = File(File(System.getProperty("user.home")), "openai.key").readText().trim()
        ) {
            override fun hands() = mapOf(
                "tools" to Tools() as Object,
            ).asJava

            override fun heart(hands: java.util.Map<String, Object>): Heart = GroovyInterpreter(hands)
        }.start(port)
        Desktop.getDesktop().browse(URI(baseURL))
        server.join()
    }
}