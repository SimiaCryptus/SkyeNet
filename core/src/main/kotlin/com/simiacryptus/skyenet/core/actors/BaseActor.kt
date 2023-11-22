package com.simiacryptus.skyenet.core.actors

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ClientUtil.toContentList
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.models.OpenAIModel
import com.simiacryptus.jopenai.models.OpenAITextModel

abstract class BaseActor<T>(
    open val prompt: String,
    val name: String? = null,
    val model: OpenAITextModel = ChatModels.GPT35Turbo,
    val temperature: Double = 0.3,
) {
    abstract fun answer(vararg messages: com.simiacryptus.jopenai.ApiModel.ChatMessage, api: API): T
    open fun response(vararg messages: com.simiacryptus.jopenai.ApiModel.ChatMessage, model: OpenAIModel = this.model, api: API) = (api as OpenAIClient).chat(
        com.simiacryptus.jopenai.ApiModel.ChatRequest(
            messages = ArrayList(messages.toList()),
            temperature = temperature,
            model = this.model.modelName,
        ),
        model = this.model
    )
    open fun answer(vararg questions: String, api: API): T = answer(*chatMessages(*questions), api = api)

    open fun chatMessages(vararg questions: String) = arrayOf(
        com.simiacryptus.jopenai.ApiModel.ChatMessage(
            role = com.simiacryptus.jopenai.ApiModel.Role.system,
            content = prompt.toContentList()
        ),
    ) + questions.map {
        com.simiacryptus.jopenai.ApiModel.ChatMessage(
            role = com.simiacryptus.jopenai.ApiModel.Role.user,
            content = it.toContentList()
        )
    }

}