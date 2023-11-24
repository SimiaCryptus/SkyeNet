package com.simiacryptus.skyenet.core.actors.record

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel.ChatMessage
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.models.ChatModels
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
    autoEvaluate = inner.autoEvaluate,
) {
    override fun answer(vararg messages: ChatMessage, api: API) =
        functionInterceptor.wrap(messages.toList().toTypedArray()) {
            inner.answer(*it, api = api)
        }

    override fun response(
        vararg messages: ChatMessage,
        model: OpenAIModel,
        api: API
    ) = functionInterceptor.wrap(
        messages.toList().toTypedArray(),
        model
    ) { messages: Array<ChatMessage>,
        model: OpenAIModel ->
        inner.response(*messages, model = model, api = api)
    }


    override fun answer(vararg questions: String, api: API) = functionInterceptor.wrap(questions) {
        inner.answer(*it, api = api)
    }

    override fun answerWithPrefix(
        codePrefix: String,
        vararg messages: ChatMessage,
        api: API
    ) = functionInterceptor.wrap(
        messages.toList().toTypedArray(),
        codePrefix
    ) { messages: Array<ChatMessage>,
        codePrefix: String ->
        inner.answerWithPrefix(codePrefix, *messages, api = api)
    }

    override fun answerWithAutoEval(
        vararg messages: String,
        api: API,
        codePrefix: String
    ) = functionInterceptor.wrap(
        messages.toList().toTypedArray(),
        codePrefix
    ) { messages: Array<String>,
        codePrefix: String ->
        inner.answerWithAutoEval(*messages, api = api, codePrefix = codePrefix)
    }

    override fun answerWithAutoEval(
        vararg messages: ChatMessage,
        api: API,
        codePrefix: String,
    ) = functionInterceptor.wrap(messages.toList().toTypedArray()) {
        inner.answerWithAutoEval(*messages, api = api, codePrefix = codePrefix)
    }

    override fun implement(
        self: CodeResult,
        brain: OpenAIClient,
        messages: Array<out ChatMessage>,
        codePrefix: String,
        model: ChatModels
    ) = functionInterceptor.wrap(
        messages.toList().toTypedArray(),
        codePrefix
    ) { messages: Array<ChatMessage>,
        codePrefix: String ->
        inner.implement(self, brain, messages, codePrefix, inner.model)
    }

    override fun validateAndFix(
        self: CodeResult,
        initialCode: String,
        codePrefix: String,
        brain: OpenAIClient,
        messages: Array<out ChatMessage>,
        model: ChatModels
    ) = functionInterceptor.wrap(
        messages.toList().toTypedArray(),
        initialCode,
        codePrefix
    ) { messages: Array<ChatMessage>,
        initialCode: String,
        codePrefix: String ->
        inner.validateAndFix(self, initialCode, codePrefix, brain, messages, inner.model) ?: ""
    }

    override fun execute(code: String) = functionInterceptor.wrap(code) {
        inner.execute(it)
    }
}