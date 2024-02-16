package com.simiacryptus.skyenet.core.actors

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.ApiModel.ChatMessage
import com.simiacryptus.jopenai.ApiModel.ImageGenerationRequest
import com.simiacryptus.jopenai.GPT4Tokenizer
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.models.ImageModels
import com.simiacryptus.jopenai.util.ClientUtil.toChatMessage
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import java.awt.image.BufferedImage
import java.net.URL
import javax.imageio.ImageIO

open class ImageActor(
    prompt: String = "Transform the user request into an image generation prompt that the user will like",
    name: String? = null,
    textModel: ChatModels = ChatModels.GPT35Turbo,
    val imageModel: ImageModels = ImageModels.DallE2,
    temperature: Double = 0.3,
    val width: Int = 1024,
    val height: Int = 1024,
) : BaseActor<List<String>, ImageResponse>(
    prompt = prompt,
    name = name,
    model = textModel,
    temperature = temperature,
) {
    override fun chatMessages(questions: List<String>) = arrayOf(
        ChatMessage(
            role = ApiModel.Role.system,
            content = prompt.toContentList()
        ),
    ) + questions.map {
        ChatMessage(
            role = ApiModel.Role.user,
            content = it.toContentList()
        )
    }

    inner class ImageResponseImpl(
        override val text: String,
        private val api: API
    ) : ImageResponse {
        private val _image: BufferedImage by lazy { render(text, api) }
        override val image: BufferedImage get() = _image
    }

    open fun render(
        text: String,
        api: API,
    ): BufferedImage {
        val url = (api as OpenAIClient).createImage(
            ImageGenerationRequest(
                prompt = text,
                model = imageModel.modelName,
                size = "${width}x$height"
            )
        ).data.first().url
        return ImageIO.read(URL(url))
    }
    private val codex = GPT4Tokenizer(false)

    override fun respond(input: List<String>, api: API, vararg messages: ChatMessage): ImageResponse {
        var text = response(*messages, api = api).choices.first().message?.content
            ?: throw RuntimeException("No response")
        while (imageModel.maxPrompt <= text.length) {
            text = response(
                *listOf(
                    messages.toList(),
                    listOf(
                        text.toChatMessage(),
                        "Please shorten the description".toChatMessage(),
                    ),
                ).flatten().map { it as ChatMessage }.toTypedArray(),
                model = imageModel,
                api = api
            ).choices.first().message?.content ?: throw RuntimeException("No response")
        }
        return ImageResponseImpl(text, api = api)
    }

    override fun withModel(model: ChatModels): ImageActor = ImageActor(
        prompt = prompt,
        name = name,
        textModel = model,
        imageModel = imageModel,
        temperature = temperature,
        width = width,
        height = height,
    )

    fun withModel(model: ImageModels): ImageActor = ImageActor(
        prompt = prompt,
        name = name,
        textModel = this.model,
        imageModel = model,
        temperature = temperature,
        width = width,
        height = height,
    )
}

interface ImageResponse {
    val text: String
    val image: BufferedImage
}