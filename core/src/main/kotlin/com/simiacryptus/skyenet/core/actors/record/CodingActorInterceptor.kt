package com.simiacryptus.skyenet.core.actors.record

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel.ChatMessage
import com.simiacryptus.jopenai.models.OpenAIModel
import com.simiacryptus.skyenet.core.actors.CodingActor
import com.simiacryptus.skyenet.core.util.FunctionWrapper

class CodingActorInterceptor(
    val inner: CodingActor,
    private val functionInterceptor: FunctionWrapper,
) : CodingActor(
    interpreterClass = inner.interpreterClass,
    symbols = inner.symbols,
    describer = inner.describer,
    name = inner.name,
    details = inner.details,
    model = inner.model,
    fallbackModel = inner.fallbackModel,
    temperature = inner.temperature,
) {
    override fun response(
        vararg input: ChatMessage,
        model: OpenAIModel,
        api: API
    ) = functionInterceptor.wrap(
        input.toList().toTypedArray(),
        model
    ) { messages: Array<ChatMessage>,
        model: OpenAIModel ->
        inner.response(*messages, model = model, api = api)
    }


    override fun respond(input: CodeRequest, api: API, vararg messages: ChatMessage) = inner.CodeResultImpl(
        messages = messages,
        input = input,
        api = api as com.simiacryptus.jopenai.OpenAIClient,
        givenCode = super.respond(input = input, api = api, *messages).code,
    )

    override fun execute(prefix: String, code: String) =
        functionInterceptor.wrap(prefix, code)
        { prefix, code -> inner.execute(prefix, code) }
}