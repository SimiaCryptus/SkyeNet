package com.simiacryptus.skyenet.apps.general.parsers

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.skyenet.core.actors.ParsedActor
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.parquet.avro.AvroParquetWriter
import org.apache.parquet.hadoop.ParquetWriter
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import java.io.File

open class DefaultParsingModel(
    private val chatModels: ChatModels,
    private val temperature: Double
) : ParsingModel {

    override fun merge(
        runningDocument: ParsingModel.DocumentData,
        newData: ParsingModel.DocumentData
    ) : ParsingModel.DocumentData {
        val runningDocument = runningDocument as DocumentData
        val newData = newData as DocumentData
        return DocumentData(
            id = newData.id ?: runningDocument.id,
            content = mergeContent(runningDocument.content, newData.content).takeIf { it.isNotEmpty() },
            entities = mergeEntities(runningDocument.entities, newData.entities).takeIf { it.isNotEmpty() },
            metadata = mergeMetadata(runningDocument.metadata, newData.metadata)
        )
    }

    protected open fun mergeMetadata(existing: DocumentMetadata?, new: DocumentMetadata?): DocumentMetadata {
        return DocumentMetadata(
            title = new?.title ?: existing?.title,
            keywords = ((existing?.keywords ?: emptyList()) + (new?.keywords ?: emptyList())).distinct(),
            properties = ((existing?.properties ?: emptyMap()) + (new?.properties ?: emptyMap())).takeIf { it.isNotEmpty() }
        )
    }

    protected open fun mergeContent(
        existingContent: List<ContentData>?,
        newContent: List<ContentData>?
    ): List<ContentData> {
        val mergedContent = (existingContent ?: emptyList()).toMutableList()
        (newContent ?: emptyList()).forEach { newItem ->
            val existingIndex = mergedContent.indexOfFirst { it.type == newItem.type && it.text?.trim() == newItem.text?.trim() }
            if (existingIndex != -1) {
                mergedContent[existingIndex] = mergeContentData(mergedContent[existingIndex], newItem)
            } else {
                mergedContent.add(newItem)
            }
        }
        return mergedContent
    }

    protected open fun mergeContentData(existing: ContentData, new: ContentData) = existing.copy(
        content = mergeContent(existing.content, new.content).takeIf { it.isNotEmpty() },
        entities = ((existing.entities ?: emptyList()) + (new.entities ?: emptyList())).distinct()
            .takeIf { it.isNotEmpty() },
        tags = ((existing.tags ?: emptyList()) + (new.tags ?: emptyList())).distinct().takeIf { it.isNotEmpty() }
    )

    protected open fun mergeEntities(
        existingEntities: Map<String, EntityData>?,
        newEntities: Map<String, EntityData>?
    ) = ((existingEntities?.keys ?: emptySet()) + (newEntities?.keys ?: emptySet())).associateWith { key ->
        val existing = existingEntities?.get(key)
        val new = newEntities?.get(key)
        when {
            existing == null -> new!!
            new == null -> existing
            else -> mergeEntityData(existing, new)
        }
    }

    protected open fun mergeEntityData(existing: EntityData, new: EntityData) = existing.copy(
        aliases = ((existing.aliases ?: emptyList()) + (new.aliases ?: emptyList())).distinct()
            .takeIf { it.isNotEmpty() },
        properties = ((existing.properties ?: emptyMap()) + (new.properties ?: emptyMap())).takeIf { it.isNotEmpty() },
        relations = ((existing.relations ?: emptyMap()) + (new.relations ?: emptyMap())).takeIf { it.isNotEmpty() },
        type = new.type ?: existing.type
    )

    open val promptSuffix = """
        |Parse the text into a hierarchical structure that describes the content of the page:
        |1. Separate the content into sections, paragraphs, statements, etc.
        |2. The final level of the hierarchy should contain singular, short, standalone sentences.
        |3. Capture any entities, relationships, and properties that can be extracted from the text of the current page(s).
        |4. For each entity, include mentions with their exact text and location (start and end indices) in the document.
        |5. Extract document metadata such as title, author, creation date, and keywords if available.
        |6. Assign relevant tags to each content section to improve searchability and categorization.
        |7. Do not copy data from the accumulated document JSON to your response; it is provided for context only.
        """.trimMargin()
    open val exampleInstance = DocumentData()
    override fun getParser(api: API): (String) -> DocumentData {
        val parser = ParsedActor(
            resultClass = DocumentData::class.java,
            exampleInstance = exampleInstance,
            prompt = "",
            parsingModel = chatModels,
            temperature = temperature
        ).getParser(
            api, promptSuffix = promptSuffix
        )
        return { text -> parser.apply(text) }
    }

    override fun newDocument() = DocumentData()

    data class DocumentData(
        @Description("Document/Page identifier") val id: String? = null,
        @Description("Entities extracted") val entities: Map<String, EntityData>? = null,
        @Description("Hierarchical structure and data") val content: List<ContentData>? = null,
        @Description("Document metadata") val metadata: DocumentMetadata? = null
    ) : ParsingModel.DocumentData

    data class EntityData(
        @Description("Aliases for the entity") val aliases: List<String>? = null,
        @Description("Entity attributes extracted from the page") val properties: Map<String, Any>? = null,
        @Description("Entity relationships extracted from the page") val relations: Map<String, String>? = null,
        @Description("Entity type (e.g., person, organization, location)") val type: String? = null
    )

    data class ContentData(
        @Description("Content type, e.g. heading, paragraph, statement, list") val type: String = "",
        @Description("Brief, self-contained text either copied, paraphrased, or summarized") val text: String? = null,
        @Description("Sub-elements") val content: List<ContentData>? = null,
        @Description("Related entities by ID") val entities: List<String>? = null,
        @Description("Tags - related topics and non-entity indexing") val tags: List<String>? = null
    )
    data class DocumentMetadata(
        @Description("Document title") val title: String? = null,
        @Description("Keywords or tags associated with the document") val keywords: List<String>? = null,
        @Description("Other metadata") val properties: Map<String, Any>? = null,
    )

    companion object {
        val log = org.slf4j.LoggerFactory.getLogger(DefaultParsingModel::class.java)
        
        fun saveAsParquet(document: DocumentData, outputPath: String) {
            val schema = Schema.Parser().parse(File("document_schema.avsc"))
            val rows = mutableListOf<GenericData.Record>()
            fun processContent(content: ContentData, parentId: String? = null) {
                val record = GenericData.Record(schema)
                record.put("id", content.hashCode().toString())
                record.put("parent_id", parentId)
                record.put("type", content.type)
                record.put("text", content.text)
                record.put("entities", content.entities?.joinToString(","))
                record.put("tags", content.tags?.joinToString(","))
                rows.add(record)
                content.content?.forEach { childContent ->
                    processContent(childContent, content.hashCode().toString())
                }
            }
            document.content?.forEach { processContent(it) }
            document.entities?.forEach { (entityId, entityData) ->
                val record = GenericData.Record(schema)
                record.put("id", entityId)
                record.put("type", "entity")
                record.put("text", entityData.aliases?.joinToString(", "))
                record.put("properties", entityData.properties?.entries?.joinToString(", ") { "${it.key}:${it.value}" })
                record.put("relations", entityData.relations?.entries?.joinToString(", ") { "${it.key}:${it.value}" })
                rows.add(record)
            }
            val conf = Configuration()
            val writer: ParquetWriter<GenericData.Record> = AvroParquetWriter.builder<GenericData.Record>(Path(outputPath))
                .withSchema(schema)
                .withConf(conf)
                .withCompressionCodec(CompressionCodecName.SNAPPY)
                .build()
            rows.forEach { writer.write(it) }
            writer.close()
        }
    }

}