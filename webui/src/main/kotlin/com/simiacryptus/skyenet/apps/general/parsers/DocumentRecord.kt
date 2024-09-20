package com.simiacryptus.skyenet.apps.general.parsers

import com.simiacryptus.jopenai.models.ApiModel
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.models.EmbeddingModels
import com.simiacryptus.util.JsonUtil
import java.io.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

data class DocumentRecord(
    val id: String,
    val parentId: String?,
    val type: String,
    val text: String?,
    val entities: String?,
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
        out.writeObject(entities)
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
            entities,
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

        fun saveAsBinary(
            openAIClient: OpenAIClient,
            outputPath: String,
            pool: ExecutorService,
            vararg inputPaths: String
        ) {
            val records = mutableListOf<DocumentRecord>()
            inputPaths.forEach { inputPath ->
                processDocument(
                    inputPath,
                    JsonUtil.fromJson(File(inputPath).readText(), DefaultParsingModel.DocumentData::class.java),
                    records,
                    openAIClient,
                    pool
                )
            }
            writeBinary(outputPath, records)
        }

        private fun processDocument(
            inputPath: String,
            document: DefaultParsingModel.DocumentData,
            records: MutableList<DocumentRecord>,
            openAIClient: OpenAIClient,
            pool: ExecutorService
        ) {
            fun processContent(content: DefaultParsingModel.ContentData, parentId: String? = null, depth: Int = 0, path: String = "") {
                val record = DocumentRecord(
                    id = content.hashCode().toString(),
                    parentId = parentId,
                    type = content.type,
                    text = content.text,
                    entities = content.entities?.joinToString(","),
                    tags = content.tags?.joinToString(","),
                    sourcePath = inputPath,
                    depth = depth,
                    jsonPath = path,
                    vector = null,
                    properties = null,
                    relations = null
                )
                records.add(record)
                content.content?.forEachIndexed { index, childContent ->
                    processContent(childContent, content.hashCode().toString(), depth + 1, "$path.content[$index]")
                }
            }
            document.content?.forEachIndexed { index, content ->
                processContent(content, null, 0, "content[$index]")
            }
            document.entities?.forEach { (entityId, entityData) ->
                records.add(DocumentRecord(
                    id = entityId,
                    parentId = null,
                    type = "entity",
                    text = "Entity ${entityData.type}: ${entityData.aliases?.joinToString(", ")}",
                    entities = null,
                    tags = null,
                    sourcePath = inputPath,
                    depth = -1,  // Use -1 to indicate it's an entity, not part of the content hierarchy
                    jsonPath = "entities.$entityId",
                    vector = null,
                    properties = entityData.properties?.entries?.joinToString(", ") { "${it.key}:${it.value}" },
                    relations = entityData.relations?.entries?.joinToString(", ") { "${it.key}:${it.value}" }
                ))
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
                    DefaultParsingModel.log.error("Error processing entity", e)
                }
            }
        }

        private fun writeBinary(outputPath: String, records: List<DocumentRecord>) {
            DefaultParsingModel.log.info("Writing ${records.size} records to $outputPath")
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
                            "", null, "", null, null, null,
                            "", 0, "", DoubleArray(0), null, null
                        ).readObject(input))
                }
            }
            return records
        }
    }
}