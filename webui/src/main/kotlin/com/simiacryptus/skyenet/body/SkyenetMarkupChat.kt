package com.simiacryptus.skyenet.body

import com.simiacryptus.openai.OpenAIClient
import org.apache.commons.io.FileUtils
import org.eclipse.jetty.webapp.WebAppContext
import java.io.File

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
            override fun renderResponse(raw: String): String {
                val rendered = super.renderResponse(raw)
                return postRender(raw, rendered)
            }
        }
        handler.setDelegate(basicChatSession)
        return handler
    }

    open fun postRender(raw: String, rendered: String) = rendered

}