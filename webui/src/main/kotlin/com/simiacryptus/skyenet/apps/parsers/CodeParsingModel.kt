package com.simiacryptus.skyenet.apps.parsers

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.models.ChatModel
import com.simiacryptus.skyenet.core.actors.ParsedActor

open class CodeParsingModel(
    private val parsingModel: ChatModel,
    private val temperature: Double
) : ParsingModel {

    override fun merge(
        runningDocument: ParsingModel.DocumentData,
        newData: ParsingModel.DocumentData
    ): ParsingModel.DocumentData {
        val runningDocument = runningDocument as CodeData
        val newData = newData as CodeData
        return CodeData(
            id = newData.id ?: runningDocument.id,
            content = mergeContent(runningDocument.content, newData.content).takeIf { it.isNotEmpty() },
            entities = mergeEntities(runningDocument.entities, newData.entities).takeIf { it.isNotEmpty() },
            metadata = mergeMetadata(runningDocument.metadata, newData.metadata)
        )
    }

    protected open fun mergeMetadata(existing: CodeMetadata?, new: CodeMetadata?): CodeMetadata {
        return CodeMetadata(
            language = new?.language ?: existing?.language,
            libraries = ((existing?.libraries ?: emptyList()) + (new?.libraries ?: emptyList())).distinct(),
            properties = ((existing?.properties ?: emptyMap()) + (new?.properties ?: emptyMap())).takeIf { it.isNotEmpty() }
        )
    }

    protected open fun mergeContent(
        existingContent: List<CodeContent>?,
        newContent: List<CodeContent>?
    ): List<CodeContent> {
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

    protected open fun mergeContentData(existing: CodeContent, new: CodeContent) = existing.copy(
        content = mergeContent(existing.content, new.content).takeIf { it.isNotEmpty() },
        entities = ((existing.entities ?: emptyList()) + (new.entities ?: emptyList())).distinct()
            .takeIf { it.isNotEmpty() },
        tags = ((existing.tags ?: emptyList()) + (new.tags ?: emptyList())).distinct().takeIf { it.isNotEmpty() }
    )

    protected open fun mergeEntities(
        existingEntities: Map<String, CodeEntity>?,
        newEntities: Map<String, CodeEntity>?
    ) = ((existingEntities?.keys ?: emptySet()) + (newEntities?.keys ?: emptySet())).associateWith { key ->
        val existing = existingEntities?.get(key)
        val new = newEntities?.get(key)
        when {
            existing == null -> new!!
            new == null -> existing
            else -> mergeEntityData(existing, new)
        }
    }

    protected open fun mergeEntityData(existing: CodeEntity, new: CodeEntity) = existing.copy(
        aliases = ((existing.aliases ?: emptyList()) + (new.aliases ?: emptyList())).distinct()
            .takeIf { it.isNotEmpty() },
        properties = ((existing.properties ?: emptyMap()) + (new.properties ?: emptyMap())).takeIf { it.isNotEmpty() },
        relations = ((existing.relations ?: emptyMap()) + (new.relations ?: emptyMap())).takeIf { it.isNotEmpty() },
        type = new.type ?: existing.type
    )

    open val promptSuffix = """
Parse the code into a structured format that describes its components:
1. Identify functions, classes, and other code structures.
2. Extract comments and document them with their associated code.
3. Capture any dependencies or libraries used in the code.
4. Extract metadata such as programming language and version if available.
5. Assign relevant tags to each code section to improve searchability and categorization.
6. Do not copy data from the accumulated code JSON to your response; it is provided for context only.
        """.trimMargin()

    open val exampleInstance = CodeData()

    override fun getParser(api: API): (String) -> CodeData {
        val parser = ParsedActor(
            resultClass = CodeData::class.java,
            exampleInstance = exampleInstance,
            prompt = "",
            parsingModel = parsingModel,
            temperature = temperature
        ).getParser(
            api, promptSuffix = promptSuffix
        )
        return { text -> parser.apply(text) }
    }

    override fun newDocument() = CodeData()

    data class CodeData(
        @Description("Code identifier") override val id: String? = null,
        @Description("Entities extracted") val entities: Map<String, CodeEntity>? = null,
        @Description("Hierarchical structure and data") override val content: List<CodeContent>? = null,
        @Description("Code metadata") override val metadata: CodeMetadata? = null
    ) : ParsingModel.DocumentData

    data class CodeEntity(
        @Description("Aliases for the entity") val aliases: List<String>? = null,
        @Description("Entity attributes extracted from the code") val properties: Map<String, Any>? = null,
        @Description("Entity relationships extracted from the code") val relations: Map<String, String>? = null,
        @Description("Entity type (e.g., function, class, variable)") val type: String? = null
    )

    data class CodeContent(
        @Description("Content type, e.g. function, class, comment") override val type: String = "",
        @Description("Brief, self-contained text either copied, paraphrased, or summarized") override val text: String? = null,
        @Description("Sub-elements") override val content: List<CodeContent>? = null,
        @Description("Related entities by ID") val entities: List<String>? = null,
        @Description("Tags - related topics and non-entity indexing") override val tags: List<String>? = null
    ) : ParsingModel.ContentData

    data class CodeMetadata(
        @Description("Programming language") val language: String? = null,
        @Description("Libraries or dependencies associated with the code") val libraries: List<String>? = null,
        @Description("Other metadata") val properties: Map<String, Any>? = null,
    ) : ParsingModel.DocumentMetadata

    companion object {
        val log = org.slf4j.LoggerFactory.getLogger(CodeParsingModel::class.java)
    }
}