package com.simiacryptus.skyenet.actors

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.models.OpenAITextModel

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

    override fun answer(vararg questions: String, api: API): String = answer(*chatMessages(*questions), api = api)

    override fun answer(vararg messages: com.simiacryptus.jopenai.ApiModel.ChatMessage, api: API): String = response(*messages, api = api).choices.first().message?.content ?: throw RuntimeException("No response")
}
