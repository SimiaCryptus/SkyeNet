package com.simiacryptus.skyenet.core.actors

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel.ChatMessage
import com.simiacryptus.jopenai.ApiModel.ImageGenerationRequest
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.models.ImageModels
import java.awt.image.BufferedImage

open class ImageActor(
    prompt: String = "Transform the user request into an image generation prompt that the user will like",
    name: String? = null,
    textModel: ChatModels = ChatModels.GPT35Turbo,
    val imageModel: ImageModels = ImageModels.DallE2,
    temperature: Double = 0.3,
    val width: Int = 1024,
    val height: Int = 1024,
) : BaseActor<ImageResponse>(
    prompt = prompt,
    name = name,
    model = textModel,
    temperature = temperature,
) {
    private inner class ImageResponseImpl(vararg messages: ChatMessage, val api: API) : ImageResponse {

        private val _text: String by lazy { response(*messages, api = api).choices.first().message?.content ?: throw RuntimeException("No response") }
        override fun getText(): String = _text
        override fun getImage(): BufferedImage {
            val url = (api as OpenAIClient).createImage(
                ImageGenerationRequest(
                    prompt = getText(),
                    model = imageModel.modelName,
                    size = "${width}x$height"
                )
            ).data.first().url
            return javax.imageio.ImageIO.read(java.net.URL(url))
        }
    }

    override fun answer(vararg messages: ChatMessage, api: API): ImageResponse {
        return ImageResponseImpl(*messages, api = api)
    }
}

interface ImageResponse {
    fun getText(): String
    fun getImage(): BufferedImage
}