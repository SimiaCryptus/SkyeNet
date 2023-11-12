package com.simiacryptus.skyenet.webui

import com.simiacryptus.openai.OpenAIClient

abstract class ChatSession(
    val parent: ApplicationBase,
    sessionId: String,
    var model: OpenAIClient.Model = OpenAIClient.Models.GPT35Turbo,
    private var visiblePrompt: String,
    private var hiddenPrompt: String,
    private var systemPrompt: String,
    val api: OpenAIClient,
) : PersistentSessionBase(sessionId, parent.sessionDataStorage) {

    init {
        if (visiblePrompt.isNotBlank()) send("""aaa,<div>${visiblePrompt}</div>""")
    }

    open val messages = listOf(
        OpenAIClient.ChatMessage(OpenAIClient.ChatMessage.Role.system, systemPrompt),
        OpenAIClient.ChatMessage(OpenAIClient.ChatMessage.Role.assistant, hiddenPrompt),
    ).toMutableList()

    @Synchronized
    override fun run(userMessage: String, socket: MessageWebSocket) {
        var responseContents = divInitializer()
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

    open fun renderResponse(response: String) = """<pre>$response</pre>"""

    open fun onResponse(response: String, responseContents: String) {}

    open val newChatRequest: OpenAIClient.ChatRequest
        get() {
            val chatRequest = OpenAIClient.ChatRequest()
            chatRequest.model = model.modelName
            chatRequest.temperature = parent.temperature
            chatRequest.messages = messages.toTypedArray()
            return chatRequest
        }

    companion object {
        fun divInitializer(operationID: String = randomID()): String {
            return """$operationID,<button class="cancel-button" data-id="$operationID">&times;</button>"""
        }

        fun randomID() = (0..5).map { ('a'..'z').random() }.joinToString("")
    }

}