package com.simiacryptus.skyenet.core.actors

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.models.OpenAIModel

abstract class BaseActor<I,R>(
    open val prompt: String,
    val name: String? = null,
    val model: ChatModels = ChatModels.GPT35Turbo,
    val temperature: Double = 0.3,
) {
    abstract fun answer(vararg messages: ApiModel.ChatMessage, input: I, api: API): R
    open fun response(vararg input: ApiModel.ChatMessage, model: OpenAIModel = this.model, api: API) = (api as OpenAIClient).chat(
        ApiModel.ChatRequest(
            messages = ArrayList(input.toList()),
            temperature = temperature,
            model = this.model.modelName,
        ),
        model = this.model
    )
    open fun answer(input: I, api: API): R = answer(*chatMessages(input), input=input, api = api)

    abstract fun chatMessages(questions: I): Array<ApiModel.ChatMessage>
    abstract fun withModel(model: ChatModels): BaseActor<I,R>
}