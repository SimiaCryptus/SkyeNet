package com.simiacryptus.skyenet.webui.util

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.models.EmbeddingModels
import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.core.platform.ApplicationServices.cloud
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationInterface

class TensorflowProjector(
    val api: API,
    val dataStorage: StorageInterface,
    val sessionID: Session,
    val session: ApplicationInterface,
    val userId: User?,
) {

    private fun toVectorMap(vararg words: String): Map<String, DoubleArray> {
        val vectors = words.map { word ->
            word to (api as OpenAIClient).createEmbedding(
                com.simiacryptus.jopenai.ApiModel.EmbeddingRequest(
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
        val sessionDir = dataStorage.getSessionDir(userId, sessionID)
        sessionDir.resolve(vectorFileName).writeText(vectorTsv)
        sessionDir.resolve(metadataFileName).writeText(metadataTsv)
        //val vectorURL = """$host/$appPath/fileIndex/$sessionID/$vectorFileName"""
        val vectorURL = cloud!!.upload("projector/$sessionID/$vectorFileName", "text/plain", vectorTsv)
        //var metadataURL = """$host/$appPath/fileIndex/$sessionID/$metadataFileName"""
        val metadataURL = cloud!!.upload("projector/$sessionID/$metadataFileName", "text/plain", metadataTsv)
        val projectorConfig = JsonUtil.toJson(
            mapOf(
                "embeddings" to listOf(
                    mapOf(
                        "tensorName" to "embedding",
                        "tensorShape" to listOf(vectorMap.values.first().size, 1),
                        "tensorPath" to vectorURL,
                        "metadataPath" to metadataURL,
                    )
                )
            )
        )
        sessionDir.resolve(configFileName).writeText(projectorConfig)
        //val configURL = """$host/$appPath/fileIndex/$sessionID/projector-config.json"""
        val configURL = cloud!!.upload("projector/$sessionID/$configFileName", "application/json", projectorConfig)
        return """
            <a href="$configURL">Projector Config</a>
            <a href="$vectorURL">Vectors</a>
            <a href="$metadataURL">Metadata</a>
            <a href="https://projector.tensorflow.org/?config=$configURL">Projector</a>
            <iframe src="https://projector.tensorflow.org/?config=$configURL" width="100%" height="500px"></iframe>
            """.trimIndent()
    }

}