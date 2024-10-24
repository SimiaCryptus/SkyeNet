package com.simiacryptus.skyenet.apps.parse

import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.models.ApiModel
import com.simiacryptus.jopenai.models.EmbeddingModels
import com.simiacryptus.util.JsonUtil
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

data class DocumentRecord(
    val text: String?,
    val metadata: String?,
    val sourcePath: String,
    val jsonPath: String,
    var vector: DoubleArray?,
) : Serializable {
    @Throws(IOException::class)
    fun writeObject(out: ObjectOutputStream) {
        out.writeUTF(text ?: "")
        out.writeUTF(metadata ?: "")
        out.writeUTF(sourcePath)
        out.writeUTF(jsonPath)
        out.writeObject(vector)
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    fun readObject(input: ObjectInputStream): DocumentRecord {
        val text = input.readUTF().let { if (it.isEmpty()) null else it }
        val metadata = input.readUTF().let { if (it.isEmpty()) null else it }
        val sourcePath = input.readUTF()
        val jsonPath = input.readUTF()
        val vector = input.readObject() as DoubleArray
        return DocumentRecord(
            text,
            metadata,
            sourcePath,
            jsonPath,
            vector
        )
    }

    companion object {
        val log = org.slf4j.LoggerFactory.getLogger(DocumentRecord::class.java)

        fun <T> saveAsBinary(
            openAIClient: OpenAIClient,
            outputPath: String,
            pool: ExecutorService,
            vararg inputPaths: String,
        ) {
            val records = mutableListOf<DocumentRecord>()
            inputPaths.forEach { inputPath ->
                processDocument(
                    inputPath,
                    JsonUtil.fromJson(File(inputPath).readText(), Map::class.java) as T,
                    records,
                    openAIClient,
                    pool
                )
            }
            writeBinary(outputPath, records)
        }

        private fun <T> processDocument(
            inputPath: String,
            document: T,
            records: MutableList<DocumentRecord>,
            openAIClient: OpenAIClient,
            pool: ExecutorService
        ) {
            fun processContent(content: Map<String, Any>, path: String = "") {
                val record = DocumentRecord(
                    text = content["text"] as? String,
                    metadata = JsonUtil.toJson(content.filter { it.key != "text" && it.key != "content" }),
                    sourcePath = inputPath,
                    jsonPath = path,
                    vector = null
                )
                records.add(record)
                (content["content"] as? List<Map<String, Any>>)?.forEachIndexed { index, childContent ->
                    processContent(childContent, "$path.content[$index]")
                }
            }
            (document as? Map<String, Any>)?.get("content")?.let { contentList ->
                (contentList as? List<Map<String, Any>>)?.forEachIndexed { index, content ->
                    processContent(content, "content[$index]")
                }
            }
            addEmbeddings(records, pool, openAIClient)
        }

        private fun addEmbeddings(
            records: MutableList<DocumentRecord>,
            pool: ExecutorService,
            openAIClient: OpenAIClient
        ) {
            val futureList = records.map {
                pool.submit {
                    it.vector = openAIClient.createEmbedding(
                        ApiModel.EmbeddingRequest(
                            EmbeddingModels.Large.modelName, it.text
                        )
                    ).data.get(0).embedding ?: DoubleArray(0)
                }
            }.toTypedArray()
            val start = System.currentTimeMillis()
            for (future in futureList) {
                try {
                    future.get(
                        TimeUnit.MINUTES.toMillis(5) - (System.currentTimeMillis() - start),
                        TimeUnit.MILLISECONDS
                    )
                } catch (e: Exception) {
                    log.error("Error processing entity", e)
                }
            }
        }

        private fun writeBinary(outputPath: String, records: List<DocumentRecord>) {
            log.info("Writing ${records.size} records to $outputPath")
            ObjectOutputStream(FileOutputStream(outputPath)).use { out ->
                out.writeInt(records.size)
                records.forEach { it.writeObject(out) }
            }
        }

        fun readBinary(inputPath: String): List<DocumentRecord> {
            val records = mutableListOf<DocumentRecord>()
            ObjectInputStream(FileInputStream(inputPath)).use { input ->
                val size = input.readInt()
                repeat(size) {
                    records.add(
                        DocumentRecord(
                            text = null,
                            metadata = null,
                            sourcePath = "",
                            jsonPath = "",
                            vector = DoubleArray(0)
                        ).readObject(input)
                    )
                }
            }
            return records
        }
    }
}