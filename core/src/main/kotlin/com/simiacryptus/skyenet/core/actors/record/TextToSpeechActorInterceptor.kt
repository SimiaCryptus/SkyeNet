package com.simiacryptus.skyenet.core.actors.record

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.models.OpenAIModel
import com.simiacryptus.skyenet.core.actors.TextToSpeechActor
import com.simiacryptus.skyenet.core.util.FunctionWrapper

class TextToSpeechActorInterceptor(
    val inner: TextToSpeechActor,
    private val functionInterceptor: FunctionWrapper,
) : TextToSpeechActor(
  name = inner.name,
  audioModel = inner.audioModel,
  "alloy",
  1.0,
  ChatModels.GPT35Turbo,
) {
    override fun response(
        vararg input: com.simiacryptus.jopenai.ApiModel.ChatMessage,
        model: OpenAIModel,
        api: API
    ) = functionInterceptor.wrap(input.toList().toTypedArray(), model) {
        messages: Array<com.simiacryptus.jopenai.ApiModel.ChatMessage>,
        model: OpenAIModel ->
            inner.response(*messages, model = model, api = api)
    }

    override fun render(text: String, api: API) : ByteArray = functionInterceptor.wrap<String,ByteArray>(text) {
        inner.render(it, api = api)
    }
}