@file:Suppress("unused")
package com.simiacryptus.skyenet

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.sessions.BasicChat
import org.eclipse.jetty.server.Server
import java.awt.Desktop
import java.net.URI

object BasicChatTest {

    val api = OpenAIClient(OpenAIClient.keyTxt)
    val log = org.slf4j.LoggerFactory.getLogger(BasicChatTest::class.java)!!
    private const val port = 8081
    private const val baseURL = "http://localhost:$port"
    private var skyenet = BasicChat(
        applicationName = "Chat Demo", api = api
    )

    @JvmStatic
    fun main(args: Array<String>) {
        val server = Server(port)
        skyenet.configure(server, "http://localhost:$port")
        server.start()
        Desktop.getDesktop().browse(URI(baseURL))
        server.join()
    }
}