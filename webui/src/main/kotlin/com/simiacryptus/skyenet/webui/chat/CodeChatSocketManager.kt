package com.simiacryptus.skyenet.webui.chat

import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.models.OpenAITextModel
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationServer

class CodeChatSocketManager(
  session: Session,
  val language: String,
  val codeSelection: String,
  api: OpenAIClient,
  model: OpenAITextModel = ChatModels.GPT35Turbo,
  storage: StorageInterface?,
) : ChatSocketManager(
    session = session,
    model = model,
    userInterfacePrompt = """
        |# Code:
        |
        |```$language
        |$codeSelection
        |```
        |
        """.trimMargin().trim(),
    systemPrompt = """
        |You are a helpful AI that helps people with coding.
        |
        |You will be answering questions about the following code:
        |
        |```$language
        |$codeSelection
        |```
        |
        |Responses may use markdown formatting.
        """.trimMargin(),
    api = api,
    applicationClass = ApplicationServer::class.java,
    storage = storage,
) {
    override fun canWrite(user: User?): Boolean = true
}