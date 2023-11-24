package com.simiacryptus.skyenet.core.actors.record

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.models.OpenAIModel
import com.simiacryptus.skyenet.core.actors.ImageActor
import com.simiacryptus.skyenet.core.actors.ParsedResponse
import com.simiacryptus.skyenet.core.util.FunctionWrapper

class ImageActorInterceptor(
    val inner: ImageActor,
    private val functionInterceptor: FunctionWrapper,
) : ImageActor(
    prompt = inner.prompt,
    action = inner.action,
    textModel = inner.model,
    imageModel = inner.imageModel,
    temperature = inner.temperature,
    width = inner.width,
    height = inner.height,
) {
    override fun answer(vararg messages: com.simiacryptus.jopenai.ApiModel.ChatMessage, api: API) =
        functionInterceptor.wrap(messages.toList().toTypedArray()) {
            inner.answer(*it, api = api)
        }

    override fun response(
        vararg messages: com.simiacryptus.jopenai.ApiModel.ChatMessage,
        model: OpenAIModel,
        api: API
    ) = functionInterceptor.wrap(messages.toList().toTypedArray(), model) {
        messages: Array<com.simiacryptus.jopenai.ApiModel.ChatMessage>,
        model: OpenAIModel ->
            inner.response(*messages, model = model, api = api)
    }

    override fun answer(vararg questions: String, api: API) = functionInterceptor.wrap(questions) {
        inner.answer(*it, api = api)
    }

}