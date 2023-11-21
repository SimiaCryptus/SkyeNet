package com.simiacryptus.skyenet.actors

import com.simiacryptus.openai.OpenAIAPI
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.models.ChatModels
import com.simiacryptus.openai.models.OpenAITextModel

open class SimpleActor(
    prompt: String,
    name: String? = null,
    model: OpenAITextModel = ChatModels.GPT35Turbo,
    temperature: Double = 0.3,
) : BaseActor<String>(
    prompt = prompt,
    name = name,
    model = model,
    temperature = temperature,
) {

    override fun answer(vararg questions: String, api: OpenAIAPI): String = answer(*chatMessages(*questions), api = api)

    override fun answer(vararg messages: OpenAIClient.ChatMessage, api: OpenAIAPI): String = response(*messages, api = api).choices.first().message?.content ?: throw RuntimeException("No response")
}
