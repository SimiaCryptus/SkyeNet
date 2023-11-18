package com.simiacryptus.skyenet.util

import com.simiacryptus.openai.models.ChatModels
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.models.EmbeddingModels
import com.simiacryptus.skyenet.config.DataStorage
import com.simiacryptus.skyenet.session.SessionBase
import com.simiacryptus.util.JsonUtil

class EmbeddingVisualizer(
    val api: OpenAIClient,
    val dataStorage: DataStorage,
    val sessionID: String,
    val appPath: String,
    val host: String,
    val session: SessionBase,
) {

    private fun toVectorMap(vararg words: String): Map<String, DoubleArray> {
        val vectors = words.map {word ->
            word to api.createEmbedding(
                OpenAIClient.EmbeddingRequest(
                    model = EmbeddingModels.AdaEmbedding.modelName,
                    input = word,
                )
            ).data.first().embedding!!
        }
        return vectors.toMap()
    }

    fun writeTensorflowEmbeddingProjectorHtml(vararg words: String): String {
        val vectorMap = toVectorMap(*words.filter { it.isNotBlank() }.toList().toTypedArray<String>())
        val vectorTsv = vectorMap.map { (_, vector) ->
            vector.joinToString(separator = "\t") {
                "%.2E".format(it)
            }
        }.joinToString(separator = "\n")

        val metadataTsv = vectorMap.keys.joinToString(separator = "\n") {
            it.replace("\n", " ")
        }

        val vectorFileName = "vectors.tsv"
        val metadataFileName = "metadata.tsv"
        val configFileName = "projector-config.json"
        dataStorage.getSessionDir(session.userId, sessionID).resolve(vectorFileName).writeText(vectorTsv)
        dataStorage.getSessionDir(session.userId, sessionID).resolve(metadataFileName).writeText(metadataTsv)
        // projector-config.json
        val projectorConfig = JsonUtil.toJson(
            mapOf(
                "embeddings" to listOf(
                    mapOf(
                        "tensorName" to "embedding",
                        "tensorShape" to listOf(vectorMap.values.first().size, 1),
                        "tensorPath" to "$host/$appPath/fileIndex/$sessionID/$vectorFileName",
                        "metadataPath" to "$host/$appPath/fileIndex/$sessionID/$metadataFileName",
                    )
                )
            )
        )
        dataStorage.getSessionDir(session.userId, sessionID).resolve(configFileName).writeText(projectorConfig)
        return """
            <a href="$host/$appPath/fileIndex/$sessionID/projector-config.json">Projector Config</a>
            <a href="$host/$appPath/fileIndex/$sessionID/$vectorFileName">Vectors</a>
            <a href="$host/$appPath/fileIndex/$sessionID/$metadataFileName">Metadata</a>
            <a href="https://projector.tensorflow.org/?config=$host/$appPath/fileIndex/$sessionID/projector-config.json">Projector</a>
            <iframe src="https://projector.tensorflow.org/?config=$host/$appPath/fileIndex/$sessionID/projector-config.json" width="100%" height="500px"></iframe>
            """.trimIndent()
    }

}