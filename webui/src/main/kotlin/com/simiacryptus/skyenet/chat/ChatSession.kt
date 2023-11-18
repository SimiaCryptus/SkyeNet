package com.simiacryptus.skyenet.chat

import com.simiacryptus.openai.models.OpenAIModel
import com.simiacryptus.openai.models.ChatModels
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.OpenAIClientBase.Companion.toContentList
import com.simiacryptus.skyenet.ApplicationBase
import com.simiacryptus.skyenet.session.SessionBase
import com.simiacryptus.skyenet.util.MarkdownUtil

open class ChatSession(
    val parent: ChatServer,
    sessionId: String,
    var model: OpenAIModel = ChatModels.GPT35Turbo,
    private var visiblePrompt: String,
    private var hiddenPrompt: String,
    private var systemPrompt: String,
    val api: OpenAIClient,
    val temperature: Double = 0.3,
    applicationClass: Class<out ApplicationBase>,
) : SessionBase(sessionId, parent.dataStorage, userId = null, applicationClass = applicationClass) {

    init {
        if (visiblePrompt.isNotBlank()) {
            send("""aaa,<div class="initial-prompt">${visiblePrompt}</div>""")
        }
    }

    protected val messages by lazy {
        val list = listOf(
            OpenAIClient.ChatMessage(OpenAIClient.Role.system, systemPrompt.toContentList()),
        ).toMutableList()
        if(hiddenPrompt.isNotBlank()) list += OpenAIClient.ChatMessage(OpenAIClient.Role.assistant, hiddenPrompt.toContentList())
        list
    }

    @Synchronized
    override fun onRun(userMessage: String, socket: ChatSocket) {
        var responseContents = divInitializer(cancelable = false)
        responseContents += """<div class="user-message">${renderResponse(userMessage)}</div>"""
        send("""$responseContents<div class="chat-response">${ApplicationBase.spinner}</div>""")
        val response = handleMessage(userMessage, responseContents)
        if(null != response) {
            responseContents += """<div class="chat-response">${renderResponse(response)}</div>"""
            send(responseContents)
            onResponse(response, responseContents)
        }
    }

    open fun handleMessage(userMessage: String, responseContents: String): String? {
        messages += OpenAIClient.ChatMessage(OpenAIClient.Role.user, userMessage.toContentList())
        val response = getResponse()
        messages += OpenAIClient.ChatMessage(OpenAIClient.Role.assistant, response.toContentList())
        return response
    }

    open fun getResponse() = api.chat(newChatRequest, model).choices.first().message?.content.orEmpty()

    open fun renderResponse(response: String) = """<div>${MarkdownUtil.renderMarkdown(response)}</div>"""

    open fun onResponse(response: String, responseContents: String) {}

    open val newChatRequest: OpenAIClient.ChatRequest
        get() = OpenAIClient.ChatRequest(
            messages = ArrayList(messages),
            temperature = temperature,
            model = model.modelName,
        )

}