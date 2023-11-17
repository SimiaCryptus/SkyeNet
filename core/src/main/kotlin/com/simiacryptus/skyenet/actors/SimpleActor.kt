package com.simiacryptus.skyenet.actors

import com.simiacryptus.openai.Model
import com.simiacryptus.openai.Models
import com.simiacryptus.openai.OpenAIClient

open class SimpleActor(
    prompt: String,
    name: String? = null,
    model: Model = Models.GPT35Turbo,
    temperature: Double = 0.3,
) : BaseActor<String>(
    prompt = prompt,
    name = name,
    model = model,
    temperature = temperature,
) {

    override fun answer(vararg questions: String, api: OpenAIClient): String = answer(*chatMessages(*questions), api = api)

    override fun answer(vararg messages: OpenAIClient.ChatMessage, api: OpenAIClient): String = response(*messages, api = api).choices.first().message?.content ?: throw RuntimeException("No response")
}

