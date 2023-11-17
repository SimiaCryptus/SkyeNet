package com.simiacryptus.skyenet.actors

import com.simiacryptus.openai.Model
import com.simiacryptus.openai.Models
import com.simiacryptus.openai.OpenAIClient

abstract class BaseActor<T>(
    open val prompt: String,
    val name: String? = null,
    val model: Model = Models.GPT35Turbo,
    val temperature: Double = 0.3,
) {
    open fun response(vararg messages: OpenAIClient.ChatMessage, model: Model = this.model, api: OpenAIClient) = api.chat(
        OpenAIClient.ChatRequest(
            messages = ArrayList(messages.toList()),
            temperature = temperature,
            model = this.model.modelName,
        ),
        model = this.model
    )
    abstract fun answer(vararg messages: OpenAIClient.ChatMessage, api: OpenAIClient): T
    open fun answer(vararg questions: String, api: OpenAIClient): T = answer(*chatMessages(*questions), api = api)

    open fun chatMessages(vararg questions: String) = arrayOf(
        OpenAIClient.ChatMessage(
            role = OpenAIClient.ChatMessage.Role.system,
            content = prompt
        ),
    ) + questions.map {
        OpenAIClient.ChatMessage(
            role = OpenAIClient.ChatMessage.Role.user,
            content = it
        )
    }

}