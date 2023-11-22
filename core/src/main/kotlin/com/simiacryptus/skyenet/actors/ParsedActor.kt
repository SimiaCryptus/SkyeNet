package com.simiacryptus.skyenet.actors

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.models.OpenAITextModel
import com.simiacryptus.jopenai.proxy.ChatProxy
import java.util.function.Function

open class ParsedActor<T:Any>(
    val parserClass: Class<out Function<String, T>>,
    prompt: String,
    val action: String? = null,
    model: OpenAITextModel = ChatModels.GPT35Turbo,
    temperature: Double = 0.3,
) : BaseActor<ParsedResponse<T>>(
    prompt = prompt,
    name = action,
    model = model,
    temperature = temperature,
) {
    val resultClass: Class<T> by lazy { parserClass.getMethod("apply", String::class.java).returnType as Class<T> }

    private inner class ParsedResponseImpl(vararg messages: com.simiacryptus.jopenai.ApiModel.ChatMessage, api: API) : ParsedResponse<T>(resultClass) {
        private val parser: Function<String, T> = ChatProxy(
            clazz = parserClass,
            api = (api as OpenAIClient),
            model = ChatModels.GPT35Turbo,
            temperature = temperature,
        ).create()
        private val _text: String by lazy { response(*messages, api = api).choices.first().message?.content ?: throw RuntimeException("No response") }
        private val _obj: T by lazy { parser.apply(getText()) }
        override fun getText(): String = _text
        override fun getObj(clazz: Class<T>): T = _obj
    }

    override fun answer(vararg messages: com.simiacryptus.jopenai.ApiModel.ChatMessage, api: API): ParsedResponse<T> {
        return ParsedResponseImpl(*messages, api = api)
    }
}
