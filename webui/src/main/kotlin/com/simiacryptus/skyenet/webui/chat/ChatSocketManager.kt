package com.simiacryptus.skyenet.webui.chat

import com.simiacryptus.jopenai.ClientUtil.toContentList
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.models.OpenAITextModel
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.session.SocketManagerBase
import com.simiacryptus.skyenet.webui.util.MarkdownUtil

open class ChatSocketManager(
    val parent: ChatServer,
    session: Session,
    val model: OpenAITextModel = ChatModels.GPT35Turbo,
    val userInterfacePrompt: String,
    private val initialAssistantPrompt: String = "",
    val systemPrompt: String,
    val api: OpenAIClient,
    val temperature: Double = 0.3,
    applicationClass: Class<out ApplicationServer>,
) : SocketManagerBase(session, parent.dataStorage, user = null, applicationClass = applicationClass) {

    init {
        if (userInterfacePrompt.isNotBlank()) {
            send("""aaa,<div class="initial-prompt">${MarkdownUtil.renderMarkdown(userInterfacePrompt)}</div>""")
        }
    }

    protected val messages by lazy {
        val list = listOf(
            com.simiacryptus.jopenai.ApiModel.ChatMessage(com.simiacryptus.jopenai.ApiModel.Role.system, systemPrompt.toContentList()),
        ).toMutableList()
        if(initialAssistantPrompt.isNotBlank()) list += com.simiacryptus.jopenai.ApiModel.ChatMessage(com.simiacryptus.jopenai.ApiModel.Role.assistant, initialAssistantPrompt.toContentList())
        list
    }

    @Synchronized
    override fun onRun(userMessage: String, socket: ChatSocket) {
        var responseContents = divInitializer(cancelable = false)
        responseContents += """<div class="user-message">${renderResponse(userMessage)}</div>"""
        send("""$responseContents<div class="chat-response">${ApplicationServer.spinner}</div>""")
        val response = handleMessage(userMessage, responseContents)
        if(null != response) {
            responseContents += """<div class="chat-response">${renderResponse(response)}</div>"""
            send(responseContents)
            onResponse(response, responseContents)
        }
    }

    open fun handleMessage(userMessage: String, responseContents: String): String? {
        messages += com.simiacryptus.jopenai.ApiModel.ChatMessage(com.simiacryptus.jopenai.ApiModel.Role.user, userMessage.toContentList())
        val response = getResponse()
        messages += com.simiacryptus.jopenai.ApiModel.ChatMessage(com.simiacryptus.jopenai.ApiModel.Role.assistant, response.toContentList())
        return response
    }

    open fun getResponse() = api.chat(newChatRequest, model).choices.first().message?.content.orEmpty()

    open fun renderResponse(response: String) = """<div>${MarkdownUtil.renderMarkdown(response)}</div>"""

    open fun onResponse(response: String, responseContents: String) {}

    open val newChatRequest: com.simiacryptus.jopenai.ApiModel.ChatRequest
        get() = com.simiacryptus.jopenai.ApiModel.ChatRequest(
            messages = ArrayList(messages),
            temperature = temperature,
            model = model.modelName,
        )

}