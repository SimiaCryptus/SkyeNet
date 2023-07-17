package com.simiacryptus.skyenet.body

import com.simiacryptus.openai.OpenAIClient

abstract class ChatSession(
    val parent: SkyenetSessionServerBase,
    val model: OpenAIClient.Model = OpenAIClient.Models.GPT35Turbo,
    sessionId: String,
    val visiblePrompt: String,
    val hiddenPrompt: String,
    val systemPrompt: String,
) : PersistentSessionBase(sessionId, parent.sessionDataStorage) {

    init {
        if (visiblePrompt.isNotBlank()) send("""aaa,<div>${visiblePrompt}</div>""")
    }

    @Synchronized
    override fun run(userMessage: String) {
        var responseContents = initialText()
        responseContents += """<div>$userMessage</div>"""
        send("""$responseContents<div>${parent.spinner}</div>""")
        messages += OpenAIClient.ChatMessage(OpenAIClient.ChatMessage.Role.user, userMessage)
        val response = getResponse()
        messages += OpenAIClient.ChatMessage(OpenAIClient.ChatMessage.Role.assistant, response)
        responseContents += """<div>${renderResponse(response)}</div>"""
        send(responseContents)
        onResponse(response, responseContents)
    }

    open fun getResponse() = parent.api.chat(newChatRequest, model).choices.first().message?.content.orEmpty()

    open fun renderResponse(response: String) = """<pre>$response</pre>"""

    open fun onResponse(response: String, responseContents: String) {}

    val messages = listOf(
        OpenAIClient.ChatMessage(OpenAIClient.ChatMessage.Role.system, systemPrompt),
        OpenAIClient.ChatMessage(OpenAIClient.ChatMessage.Role.assistant, hiddenPrompt),
    ).toMutableList()

    open val newChatRequest: OpenAIClient.ChatRequest
        get() {
            val chatRequest = OpenAIClient.ChatRequest()
            chatRequest.model = model.modelName
            chatRequest.max_tokens = model.maxTokens
            chatRequest.temperature = parent.temperature
            chatRequest.messages = messages.toTypedArray()
            return chatRequest
        }

    companion object {
        fun initialText(): String {
            val operationID = (0..5).map { ('a'..'z').random() }.joinToString("")
            return """$operationID,<button class="cancel-button" data-id="$operationID">&times;</button>"""
        }
    }

}