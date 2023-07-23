package com.simiacryptus.skyenet.body

import com.simiacryptus.openai.OpenAIClient
import com.vladsch.flexmark.util.data.MutableDataSet
import org.apache.commons.io.FileUtils
import org.eclipse.jetty.webapp.WebAppContext
import java.awt.Desktop
import java.io.File
import java.net.URI

open class SkyenetMarkupChat(
    applicationName: String,
    baseURL: String,
    oauthConfig: String? = null,
    val visiblePrompt: String = """
                |Hello! I am here to assist you in a casual conversation! 
                |Feel free to ask me anything or just chat about your day.
                """.trimMargin(),
    val hiddenPrompt: String = """
                |I understand that the user might want to have a casual conversation. 
                |So, I'll respond in a friendly and engaging manner.
                |I will also ask questions to keep the conversation going.
                |If I mention any topics that can be cross-referenced on Wikipedia, I'll provide a link.
                |Once we have finished our conversation, I'll say goodbye.
                |
                |${visiblePrompt}
                """.trimMargin(),
    val systemPrompt: String = """
                |You are a friendly and conversational AI that engages in casual chat with users.
                |Your task is to respond to the user's messages in a friendly and engaging manner.
                |Ask questions to keep the conversation going.
                |Say goodbye when the conversation is over.
                """.trimMargin()
) : SkyenetSessionServerBase(
    applicationName = applicationName,
    baseURL = baseURL,
    oauthConfig = oauthConfig,
) {
    override val api: OpenAIClient
        get() = OpenAIClient()

    override fun configure(context: WebAppContext) {
        super.configure(context)
        if (null != oauthConfig) AuthenticatedWebsite(
            "$baseURL/oauth2callback",
            this@SkyenetMarkupChat.applicationName
        ) {
            FileUtils.openInputStream(File(oauthConfig))
        }.configure(context)
    }

    override fun newSession(sessionId: String): SessionInterface {
        val handler = MutableSessionHandler(null)
        val basicChatSession = object : ChatSessionFlexmark(
            parent = this@SkyenetMarkupChat,
            sessionId = sessionId,
            visiblePrompt = visiblePrompt,
            hiddenPrompt = hiddenPrompt,
            systemPrompt = systemPrompt,
        ) {
            override fun renderResponse(raw: String) = postRender(raw, super.renderResponse(raw))

            override fun flexmarkOptions() = editFlexmarkOptions(super.flexmarkOptions())
        }
        handler.setDelegate(basicChatSession)
        return handler
    }

    open fun postRender(raw: String, rendered: String) = rendered
    open fun editFlexmarkOptions(options: MutableDataSet = MutableDataSet()) = options

    companion object {

        val api = OpenAIClient(OpenAIClient.keyTxt)
        val log = org.slf4j.LoggerFactory.getLogger(SkyenetMarkupChat::class.java)!!
        var sessionDataStorage: SessionDataStorage? = null
        const val port = 8081
        const val baseURL = "http://localhost:$port"
        var skyenet = SkyenetMarkupChat(
            applicationName = "Chat Demo",
            baseURL = baseURL
        )

        @JvmStatic
        fun main(args: Array<String>) {
            val httpServer = skyenet.start(port)
            sessionDataStorage = skyenet.sessionDataStorage
            Desktop.getDesktop().browse(URI(baseURL))
            httpServer.join()
        }
    }

}