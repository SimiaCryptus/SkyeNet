package com.simiacryptus.skyenet.webui

import com.simiacryptus.openai.OpenAIClient

open class BasicChatSession(
    parent: SkyenetSessionServerBase,
    model: OpenAIClient.Model = OpenAIClient.Models.GPT35Turbo,
    sessionId: String,
    visiblePrompt: String = """
    |Hello! I am here to assist you in a casual conversation! 
    |Feel free to ask me anything or just chat about your day.
    """.trimMargin(),
    hiddenPrompt: String = """
    |I understand that the user might want to have a casual conversation. 
    |So, I'll respond in a friendly and engaging manner.
    |I will also ask questions to keep the conversation going.
    |Once we have finished our conversation, I'll say goodbye.
    |
    |${visiblePrompt}
    """.trimMargin(),
    systemPrompt: String = """
    |You are a friendly and conversational AI that engages in casual chat with users.
    |Your task is to respond to the user's messages in a friendly and engaging manner.
    |Ask questions to keep the conversation going.
    |Say goodbye when the conversation is over.
    """.trimMargin(),
) : ChatSession(parent, sessionId, model, visiblePrompt, hiddenPrompt, systemPrompt)