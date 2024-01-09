package com.simiacryptus.skyenet.core.actors

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel
import com.simiacryptus.jopenai.ApiModel.ChatMessage
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.jopenai.GPT4Tokenizer
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.models.AudioModels
import com.simiacryptus.jopenai.models.ChatModels

open class TextToSpeechActor(
  name: String? = null,
  val audioModel: AudioModels = AudioModels.TTS_HD,
  val voice: String = "alloy",
  val speed: Double = 1.0,
) : BaseActor<List<String>, SpeechResponse>(
  prompt = "",
  name = name,
) {
  override fun chatMessages(questions: List<String>) = questions.map {
    ChatMessage(
      role = ApiModel.Role.user,
      content = it.toContentList()
    )
  }.toTypedArray()

  inner class SpeechResponseImpl(
    val text: String,
    private val api: API
  ) : SpeechResponse {
    private val _image: ByteArray? by lazy { render(text, api) }
    override val mp3data: ByteArray? get() = _image
  }

  open fun render(
    text: String,
    api: API,
  ): ByteArray = (api as OpenAIClient).createSpeech(
    ApiModel.SpeechRequest(
      input = text,
      model = audioModel.modelName,
      voice = voice,
      speed = speed,
    )
  ) ?: throw RuntimeException("No response")

  private val codex = GPT4Tokenizer(false)

  override fun answer(vararg messages: ChatMessage, input: List<String>, api: API) =
    SpeechResponseImpl(
      messages.joinToString("\n") { it.content?.joinToString("\n") { it.text ?: "" } ?: "" },
      api = api
    )


  fun withModel(model: AudioModels): TextToSpeechActor = TextToSpeechActor(
    name = name,
    audioModel = model,
    voice = "alloy",
    speed = 1.0,
  )

  override fun withModel(model: ChatModels) = this
}

interface SpeechResponse {
  val mp3data: ByteArray?
}

