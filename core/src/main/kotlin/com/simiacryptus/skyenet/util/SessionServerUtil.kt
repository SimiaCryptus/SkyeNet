package com.simiacryptus.skyenet.util

import com.simiacryptus.openai.OpenAIClient.ChatMessage

object SessionServerUtil {
    fun getRenderedResponse(respondWithCode: List<Pair<String, String>>) =
        respondWithCode.joinToString("\n") {
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

    val logger = org.slf4j.LoggerFactory.getLogger(SessionServerUtil::class.java)

    fun getCode(language: String, textSegments: List<Pair<String, String>>) =
        textSegments.joinToString("\n") {
            if (it.first.lowercase() == "code" || it.first.lowercase() == language.lowercase()) {
                """
                    |${it.second}
                    |""".trimMargin().trim()
            } else {
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

}