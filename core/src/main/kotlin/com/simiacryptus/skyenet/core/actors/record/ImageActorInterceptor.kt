package com.simiacryptus.skyenet.core.actors.record

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.models.OpenAIModel
import com.simiacryptus.skyenet.core.actors.ImageActor
import com.simiacryptus.skyenet.core.util.FunctionWrapper
import java.awt.image.BufferedImage

class ImageActorInterceptor(
    val inner: ImageActor,
    private val functionInterceptor: FunctionWrapper,
) : ImageActor(
    prompt = inner.prompt,
    name = inner.name,
    textModel = inner.model,
    imageModel = inner.imageModel,
    temperature = inner.temperature,
    width = inner.width,
    height = inner.height,
) {
    override fun answer(vararg messages: com.simiacryptus.jopenai.ApiModel.ChatMessage, input: List<String>, api: API) =
        functionInterceptor.wrap(messages.toList().toTypedArray()) {
            inner.answer(*it, input=input, api = api)
        }

    override fun response(
        vararg input: com.simiacryptus.jopenai.ApiModel.ChatMessage,
        model: OpenAIModel,
        api: API
    ) = functionInterceptor.wrap(input.toList().toTypedArray(), model) {
        messages: Array<com.simiacryptus.jopenai.ApiModel.ChatMessage>,
        model: OpenAIModel ->
            inner.response(*messages, model = model, api = api)
    }



    override fun answer(input: List<String>, api: API) = ImageResponseImpl(functionInterceptor.wrap(input) {
        inner.answer(it, api = api).text
    }, api)

    override fun render(text: String, api: API): BufferedImage = functionInterceptor.wrap(text) {
        inner.render(it, api = api)
    }
}