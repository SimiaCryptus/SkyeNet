package com.simiacryptus.skyenet.core.actors.record

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.models.OpenAIModel
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.util.FunctionWrapper

class SimpleActorInterceptor(
    val inner: SimpleActor,
    private val functionInterceptor: FunctionWrapper,
) : SimpleActor(
    prompt = inner.prompt,
    name = inner.name,
    model = inner.model,
    temperature = inner.temperature,
) {

    override fun response(
        vararg input: com.simiacryptus.jopenai.ApiModel.ChatMessage,
        model: OpenAIModel,
        api: API
    ) = functionInterceptor.wrap(
        input.toList().toTypedArray(),
        model
    ) { messages: Array<com.simiacryptus.jopenai.ApiModel.ChatMessage>,
        model: OpenAIModel ->
        inner.response(*messages, model = model, api = api)
    }

}