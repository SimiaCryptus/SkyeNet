package com.simiacryptus.skyenet.actors

import com.simiacryptus.openai.models.OpenAIModel
import com.simiacryptus.openai.models.ChatModels
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.OpenAIClientBase.Companion.toContentList
import com.simiacryptus.openai.models.OpenAITextModel

abstract class BaseActor<T>(
    open val prompt: String,
    val name: String? = null,
    val model: OpenAITextModel = ChatModels.GPT35Turbo,
    val temperature: Double = 0.3,
) {
    open fun response(vararg messages: OpenAIClient.ChatMessage, model: OpenAIModel = this.model, api: OpenAIClient) = api.chat(
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
            role = OpenAIClient.Role.system,
            content = prompt.toContentList()
        ),
    ) + questions.map {
        OpenAIClient.ChatMessage(
            role = OpenAIClient.Role.user,
            content = it.toContentList()
        )
    }

}