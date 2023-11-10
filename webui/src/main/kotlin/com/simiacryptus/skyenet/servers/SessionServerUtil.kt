package com.simiacryptus.skyenet.servers

import com.simiacryptus.openai.OpenAIClient.ChatMessage
import com.simiacryptus.skyenet.body.OperationStatus

object SessionServerUtil {

    val logger = org.slf4j.LoggerFactory.getLogger(SessionServerUtil::class.java)

    operator fun <K, V> java.util.Map<K, V>.plus(mapOf: Map<K, V>): java.util.Map<K, V> {
        val hashMap = java.util.HashMap<K, V>()
        this.forEach(hashMap::put)
        hashMap.putAll(mapOf)
        return hashMap as java.util.Map<K, V>
    }

    fun getHistory(statuses: Collection<OperationStatus>, maxEntries: Int, maxChars: Int): List<ChatMessage> {
        val messages: java.util.ArrayList<ChatMessage> = ArrayList()
        val operationStatusList = statuses
            .filter { it.status != OperationStatus.OperationState.Pending }
            .filter { it.status != OperationStatus.OperationState.Running }
            .filter { it.status != OperationStatus.OperationState.Implemented }
            .filter { !it.instruction.startsWith("!!!") }
            .sortedBy { it.created }
            .takeLast(maxEntries)
        for (operationStatus in operationStatusList) {
            messages.addAll(operationStatus.chatMessages)
        }
        if (messages.isEmpty()) return messages
        var totalCharacters = 0
        val truncatedMessages = mutableListOf<ChatMessage>()
        for (message in messages.reversed()) {
            totalCharacters += message.content?.length ?: 0
            if (totalCharacters > maxChars) break
            truncatedMessages.add(message)
        }
        return truncatedMessages.reversed()
    }

}