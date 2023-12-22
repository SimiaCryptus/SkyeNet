package com.simiacryptus.skyenet.webui.chat

import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.ClientUtil.toContentList
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.models.OpenAITextModel
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.session.SocketManagerBase
import com.simiacryptus.skyenet.webui.util.MarkdownUtil

open class ChatSocketManager(
    session: Session,
    val model: OpenAITextModel = ChatModels.GPT35Turbo,
    val userInterfacePrompt: String,
    open val initialAssistantPrompt: String = "",
    open val systemPrompt: String,
    val api: OpenAIClient,
    val temperature: Double = 0.3,
    applicationClass: Class<out ApplicationServer>,
    val storage: StorageInterface?,
) : SocketManagerBase(session, storage, owner = null, applicationClass = applicationClass) {

    init {
        if (userInterfacePrompt.isNotBlank()) {
            send("""aaa,<div class="initial-prompt">${MarkdownUtil.renderMarkdown(userInterfacePrompt)}</div>""")
        }
    }

    protected val messages by lazy {
        val list = listOf(
            ApiModel.ChatMessage(ApiModel.Role.system, systemPrompt.toContentList()),
        ).toMutableList()
        if (initialAssistantPrompt.isNotBlank()) list +=
            ApiModel.ChatMessage(ApiModel.Role.assistant, initialAssistantPrompt.toContentList())
        list
    }

    @Synchronized
    override fun onRun(userMessage: String, socket: ChatSocket) {
        var responseContents = divInitializer(cancelable = false)
        responseContents += """<div class="user-message">${renderResponse(userMessage)}</div>"""
        send("""$responseContents<div class="chat-response">${SessionTask.spinner}</div>""")
        messages += ApiModel.ChatMessage(ApiModel.Role.user, userMessage.toContentList())
        try {
            val response = api.chat(
                ApiModel.ChatRequest(
                    messages = messages,
                    temperature = temperature,
                    model = model.modelName,
                ), model
            ).choices.first().message?.content.orEmpty()
            messages += ApiModel.ChatMessage(ApiModel.Role.assistant, response.toContentList())
            responseContents += """<div class="chat-response">${renderResponse(response)}</div>"""
            send(responseContents)
            onResponse(response, responseContents)
        } catch (e: Exception) {
            log.info("Error in chat", e)
            responseContents += """<div class="error">${e.message}</div>"""
            send(responseContents)
        }
    }

    open fun renderResponse(response: String) = """<div>${MarkdownUtil.renderMarkdown(response)}</div>"""

    open fun onResponse(response: String, responseContents: String) {}

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(ChatSocketManager::class.java)
    }
}