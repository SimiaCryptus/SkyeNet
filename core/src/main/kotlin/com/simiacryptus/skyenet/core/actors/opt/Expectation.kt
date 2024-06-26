package com.simiacryptus.skyenet.core.actors.opt

import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.models.EmbeddingModels
import com.simiacryptus.jopenai.opt.DistanceType
import org.slf4j.LoggerFactory

abstract class Expectation {
    companion object {
        private val log = LoggerFactory.getLogger(Expectation::class.java)
    }

    open class VectorMatch(private val example: String, private val metric: DistanceType = DistanceType.Cosine) :
        Expectation() {
        override fun matches(api: OpenAIClient, response: String): Boolean {
            return true
        }

        override fun score(api: OpenAIClient, response: String): Double {
            val contentEmbedding = createEmbedding(api, example)
            val promptEmbedding = createEmbedding(api, response)
            val distance = metric.distance(contentEmbedding, promptEmbedding)
            log.info(
                """Distance = $distance
                |  from "${example.replace("\n", "\\n")}" 
                |  to "${response.replace("\n", "\\n")}"
                """.trimMargin().trim()
            )
            return -distance
        }

        private fun createEmbedding(api: OpenAIClient, str: String) = api.createEmbedding(
            com.simiacryptus.jopenai.ApiModel.EmbeddingRequest(
                model = EmbeddingModels.AdaEmbedding.modelName, input = str
            )
        ).data.first().embedding!!
    }

    open class ContainsMatch(
        private val pattern: Regex,
        val critical: Boolean = true
    ) : Expectation() {
        override fun matches(api: OpenAIClient, response: String): Boolean {
            if (!critical) return true
            return _matches(response)
        }

        override fun score(api: OpenAIClient, response: String): Double {
            return if (_matches(response)) 1.0 else 0.0
        }

        private fun _matches(response: String?): Boolean {
            if (pattern.containsMatchIn(response ?: "")) return true
            log.info(
                """Failed to match ${
                    pattern.pattern.replace("\n", "\\n")
                } in ${
                    response?.replace("\n", "\\n") ?: ""
                }"""
            )
            return false
        }

    }

    abstract fun matches(api: OpenAIClient, response: String): Boolean

    abstract fun score(api: OpenAIClient, response: String): Double


}