package com.simiacryptus.skyenet.core.actors

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.models.OpenAITextModel
import com.simiacryptus.jopenai.util.ClientUtil.toContentList

open class SimpleActor(
    prompt: String,
    name: String? = null,
    model: ChatModels,
    temperature: Double = 0.3,
) : BaseActor<List<String>, String>(
    prompt = prompt,
    name = name,
    model = model,
    temperature = temperature,
) {

    override fun respond(input: List<String>, api: API, vararg messages: ApiModel.ChatMessage): String =
        response(*messages, api = api).choices.first().message?.content ?: throw RuntimeException("No response")

    override fun chatMessages(questions: List<String>) = arrayOf(
        ApiModel.ChatMessage(
            role = ApiModel.Role.system,
            content = prompt.toContentList()
        ),
    ) + questions.map {
        ApiModel.ChatMessage(
            role = ApiModel.Role.user,
            content = it.toContentList()
        )
    }

    override fun withModel(model: ChatModels): SimpleActor = SimpleActor(
        prompt = prompt,
        name = name,
        model = model,
        temperature = temperature,
    )
}
