@file:Suppress("MemberVisibilityCanBePrivate", "unused")
package com.simiacryptus.skyenet

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.body.SessionDataStorage
import com.simiacryptus.skyenet.body.SkyenetMarkupChat
import java.awt.Desktop
import java.net.URI

object FlexmarkChatTest {

    val api = OpenAIClient(OpenAIClient.keyTxt)
    val log = org.slf4j.LoggerFactory.getLogger(FlexmarkChatTest::class.java)!!
    var sessionDataStorage: SessionDataStorage? = null
    const val port = 8081
    const val baseURL = "http://localhost:$port"
    var skyenet = SkyenetMarkupChat(
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