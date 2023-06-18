package com.simiacryptus.skyenet.body

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.OpenAIClient.ChatMessage
import com.simiacryptus.openai.OpenAIClient.ChatRequest
import com.simiacryptus.util.JsonUtil
import com.simiacryptus.util.describe.TypeDescriber
import com.simiacryptus.util.describe.YamlDescriber

open class InterviewSession<T : Any>(
    private val parent: SkyenetSessionServerBase,
    val model: OpenAIClient.Model = OpenAIClient.Models.GPT35Turbo,
    sessionId: String,
    val dataClass: Class<T>,
    describer: TypeDescriber = YamlDescriber(),
    val visiblePrompt: String = """
    |Hello! I am here to assist you in specifying a ${dataClass.simpleName}! 
    |I will guide you through a series of questions to gather the necessary information. 
    |Don't worry if you're not sure about any technical details; I'm here to help!
    """.trimMargin(),
    val isFinished: (T) -> List<String>,
    val hiddenPrompt: String = """
    |I understand that the user might not know the technical details of the data structure we need to fill. 
    |So, I'll ask questions in a way that's easy for a layperson to understand and provide clarifications if needed.
    |At the end of each message, I will output the currently-accumulated JSON of the data using ```json``` blocks.
    |Whenever asking a question, I will provide some suggestions.
    |I will also ask for clarification when needed.
    |Once we have gathered all the necessary information, I'll provide you with the final JSON.
    |
    |${visiblePrompt}
    |
    |The current data structure is:
    |```json
    |${JsonUtil.toJson(dataClass.getDeclaredConstructor().newInstance())}
    |```
    """.trimMargin(),
    val systemPrompt: String = """
    |You are a friendly and conversational AI that helps interview users to assist in preparing a request data structure.
    |The data structure is defined as follows:
    |```yaml
    |${describer.describe(dataClass)}
    |```
    |The user will provide information, but it may not be in the correct format or they might not know the exact fields to fill. 
    |Your task is to ask questions and guide the user to infer and fill the correct data structure.
    |Provide a list of suggestions whenever asking a question.
    |Ask for clarification when needed.
    |At the end of each assistant chat message, print the currently-accumulated JSON of the data using ```json``` blocks.
    """.trimMargin(),
) : PersistentSessionBase(sessionId, parent.sessionDataStorage) {

    init {
        if (visiblePrompt.isNotBlank()) send("""aaa,<div>${visiblePrompt}</div>""")
    }

    @Synchronized
    override fun run(userMessage: String) {
        var messageTrail = Companion.initialText(userMessage)
        send("""$messageTrail<div>${parent.spinner}</div>""")
        messages += ChatMessage(ChatMessage.Role.user, userMessage)
        val response = parent.api.chat(chatRequest).choices?.first()?.message?.content.orEmpty()
        messages += ChatMessage(ChatMessage.Role.assistant, response)
        messageTrail += """<div><pre>$response</pre></div>"""
        send(messageTrail)

        // If the response contains a JSON (```json``` block), parse the data structure and check if it's finished
        val jsonRegex = Regex("""(?s)```json\s*(?<json>.*?)\s*```""")
        val jsonMatch = jsonRegex.find(response)
        if (jsonMatch != null) {
            val json = jsonMatch.groups["json"]!!.value
            val data = JsonUtil.fromJson<T>(json, dataClass)
            val validationResult = isFinished(data)
            if (validationResult.isEmpty()) {
                messageTrail += """<div>Finished!</div>"""
                send(messageTrail)
                onFinished(data)
            } else {
                messages += ChatMessage(ChatMessage.Role.system, "Validation errors: ${validationResult.joinToString(", ")}")
                onUpdate(data)
            }
        }
    }

    open fun onFinished(data: T) {}
    open fun onUpdate(data: T) {}

    val messages = listOf(
        ChatMessage(ChatMessage.Role.system, systemPrompt),
        ChatMessage(ChatMessage.Role.assistant, hiddenPrompt),
    ).toMutableList()

    val chatRequest: ChatRequest
        get() {
            val chatRequest = ChatRequest()
            chatRequest.model = model.modelName
            chatRequest.max_tokens = model.maxTokens
            chatRequest.temperature = parent.temperature
            chatRequest.messages = messages.toTypedArray()
            return chatRequest
        }

    companion object {
        fun initialText(userMessage: String): String {
            val operationID = (0..5).map { ('a'..'z').random() }.joinToString("")
            var messageTrail = """$operationID,<button class="cancel-button" data-id="$operationID">&times;</button>"""
            messageTrail += """<div>$userMessage</div>"""
            return messageTrail
        }
    }

}