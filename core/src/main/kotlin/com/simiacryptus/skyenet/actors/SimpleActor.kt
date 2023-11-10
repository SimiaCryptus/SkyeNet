package com.simiacryptus.skyenet.actors

import com.simiacryptus.openai.OpenAIClient

class SimpleActor(
    prompt: String,
    name: String? = null,
    api: OpenAIClient = OpenAIClient(),
    model: OpenAIClient.Models = OpenAIClient.Models.GPT35Turbo,
    temperature: Double = 0.3,
) : BaseActor<String>(
    prompt = prompt,
    name = name,
    api = api,
    model = model,
    temperature = temperature,

) {

    override fun answer(vararg questions: String): String = answer(*chatMessages(*questions))

    override fun answer(vararg messages: OpenAIClient.ChatMessage): String = response(*messages).choices.first().message?.content ?: throw RuntimeException("No response")
}

