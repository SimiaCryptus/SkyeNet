package com.simiacryptus.skyenet.chat

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.ApplicationBase
import com.simiacryptus.skyenet.session.SessionBase
import com.simiacryptus.skyenet.util.MarkdownUtil

open class ChatSession(
    val parent: ChatServer,
    sessionId: String,
    var model: OpenAIClient.Model = OpenAIClient.Models.GPT35Turbo,
    private var visiblePrompt: String,
    private var hiddenPrompt: String,
    private var systemPrompt: String,
    val api: OpenAIClient,
    val temperature: Double = 0.3,
) : SessionBase(sessionId, parent.sessionDataStorage) {

    init {
        if (visiblePrompt.isNotBlank()) {
            send("""aaa,<div>${visiblePrompt}</div>""")
        }
    }

    protected val messages by lazy {
        val list = listOf(
            OpenAIClient.ChatMessage(OpenAIClient.ChatMessage.Role.system, systemPrompt),
        ).toMutableList()
        if(hiddenPrompt.isNotBlank()) list += OpenAIClient.ChatMessage(OpenAIClient.ChatMessage.Role.assistant, hiddenPrompt)
        list
    }

    @Synchronized
    override fun onRun(userMessage: String, socket: ChatSocket) {
        var responseContents = divInitializer(cancelable = false)
        responseContents += """<div>$userMessage</div>"""
        send("""$responseContents<div>${ApplicationBase.spinner}</div>""")
        val response = handleMessage(userMessage, responseContents)
        if(null != response) {
            responseContents += """<div>${renderResponse(response)}</div>"""
            send(responseContents)
            onResponse(response, responseContents)
        }
    }

    open fun handleMessage(userMessage: String, responseContents: String): String? {
        messages += OpenAIClient.ChatMessage(OpenAIClient.ChatMessage.Role.user, userMessage)
        val response = getResponse()
        messages += OpenAIClient.ChatMessage(OpenAIClient.ChatMessage.Role.assistant, response)
        return response
    }

    open fun getResponse() = api.chat(newChatRequest, model).choices.first().message?.content.orEmpty()

    open fun renderResponse(response: String) = """<div>${MarkdownUtil.renderMarkdown(response)}</div>"""

    open fun onResponse(response: String, responseContents: String) {}

    open val newChatRequest: OpenAIClient.ChatRequest
        get() {
            val chatRequest = OpenAIClient.ChatRequest()
            chatRequest.model = model.modelName
            chatRequest.temperature = temperature
            chatRequest.messages = messages.toTypedArray()
            return chatRequest
        }

}