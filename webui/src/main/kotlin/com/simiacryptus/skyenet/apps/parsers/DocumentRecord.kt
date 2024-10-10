package com.simiacryptus.skyenet.apps.parsers

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
    val id: String,
    val parentId: String?,
    val type: String,
    val text: String?,
    val tags: String?,
    val sourcePath: String,
    val depth: Int,
    val jsonPath: String,
    var vector: DoubleArray?,
    val properties: String?,
    val relations: String?
) : Serializable {
    @Throws(IOException::class)
    fun writeObject(out: ObjectOutputStream) {
        out.writeUTF(id)
        out.writeObject(parentId)
        out.writeUTF(type)
        out.writeObject(text)
        out.writeObject(tags)
        out.writeUTF(sourcePath)
        out.writeInt(depth)
        out.writeUTF(jsonPath)
        out.writeObject(vector)
        out.writeObject(properties)
        out.writeObject(relations)
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    fun readObject(input: ObjectInputStream): DocumentRecord {
        val id = input.readUTF()
        val parentId = input.readObject() as String?
        val type = input.readUTF()
        val text = input.readObject() as String?
        val entities = input.readObject() as String?
        val tags = input.readObject() as String?
        val sourcePath = input.readUTF()
        val depth = input.readInt()
        val jsonPath = input.readUTF()
        val vector = input.readObject() as DoubleArray
        val properties = input.readObject() as String?
        val relations = input.readObject() as String?
        return DocumentRecord(
            id,
            parentId,
            type,
            text,
            tags,
            sourcePath,
            depth,
            jsonPath,
            vector,
            properties,
            relations
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
            fun processContent(content: Map<String, Any>, parentId: String? = null, depth: Int = 0, path: String = "") {
                val record = DocumentRecord(
                    id = content.hashCode().toString(),
                    parentId = parentId,
                    type = content["type"] as? String ?: "",
                    text = content["text"] as? String,
                    tags = (content["tags"] as? List<*>)?.joinToString(","),
                    sourcePath = inputPath,
                    depth = depth,
                    jsonPath = path,
                    vector = null,
                    properties = null,
                    relations = null
                )
                records.add(record)
                (content["content"] as? List<Map<String, Any>>)?.forEachIndexed { index, childContent ->
                    processContent(childContent, content.hashCode().toString(), depth + 1, "$path.content[$index]")
                }
            }
            (document as? Map<String, Any>)?.get("content")?.let { contentList ->
                (contentList as? List<Map<String, Any>>)?.forEachIndexed { index, content ->
                    processContent(content, null, 0, "content[$index]")
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
                            id = "",
                            parentId = null,
                            type = "",
                            text = null,
                            tags = null,
                            sourcePath = "",
                            depth = 0,
                            jsonPath = "",
                            vector = DoubleArray(0),
                            properties = null,
                            relations = null
                        ).readObject(input)
                    )
                }
            }
            return records
        }
    }
}