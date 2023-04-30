package com.simiacryptus.skyenet.body

import com.simiacryptus.openai.ChatMessage

object SessionServerUtil {
    fun getRenderedResponse(respondWithCode: Pair<String, List<Pair<String, String>>>) =
        respondWithCode.second.joinToString("\n") {
            var language = it.first
            if (language == "code") language = "groovy"
            if (language == "text") {
                //language=HTML
                """
                    |<div>
                    |${it.second}
                    |</div>
                    |""".trimMargin().trim()
            } else {
                //language=HTML
                """
                    |<pre><code class="language-$language">
                    |${it.second}
                    |</code></pre>
                    |""".trimMargin().trim()
            }
        }

    fun getCode(language: String, textSegments: List<Pair<String, String>>) =
        textSegments.joinToString("\n") {
            if (it.first.lowercase() == "code" || it.first.lowercase() == language.lowercase()) {
                SkyenetSessionServer.logger.debug("Selected: $language: ${it.second}")
                """
                    |${it.second}
                    |""".trimMargin().trim()
            } else {
                SkyenetSessionServer.logger.debug("Not Selected: ${it.first}: ${it.second}")
                ""
            }
        }

    operator fun <K, V> java.util.Map<K, V>.plus(mapOf: Map<K, V>): java.util.Map<K, V> {
        val hashMap = java.util.HashMap<K, V>()
        this.forEach(hashMap::put)
        hashMap.putAll(mapOf)
        return hashMap as java.util.Map<K, V>
    }

    val <K, V> Map<K, V>.asJava: java.util.Map<K, V>
        get() {
            return java.util.HashMap<K, V>().also { map ->
                this.forEach { (key, value) ->
                    map[key] = value
                }
            } as java.util.Map<K, V>
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