@file:Suppress("unused")
package com.simiacryptus.skyenet

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.body.SessionDataStorage
import com.simiacryptus.skyenet.body.SkyenetBasicChat
import java.awt.Desktop
import java.net.URI

object BasicChatTest {

    val api = OpenAIClient(OpenAIClient.keyTxt)
    val log = org.slf4j.LoggerFactory.getLogger(BasicChatTest::class.java)!!
    private var sessionDataStorage: SessionDataStorage? = null
    private const val port = 8081
    private const val baseURL = "http://localhost:$port"
    private var skyenet = SkyenetBasicChat(
        applicationName = "Chat Demo"
    )

    @JvmStatic
    fun main(args: Array<String>) {
        val httpServer = skyenet.start(port)
        sessionDataStorage = skyenet.sessionDataStorage
        Desktop.getDesktop().browse(URI(baseURL))
        httpServer.join()
    }
}