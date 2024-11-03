package com.simiacryptus.skyenet.apps.parse

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
            content_list = mergeContent(runningDocument.content_list, newData.content_list).takeIf { it.isNotEmpty() },
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
        content_list = mergeContent(existing.content_list, new.content_list).takeIf { it.isNotEmpty() },
        tags = ((existing.tags ?: emptyList()) + (new.tags ?: emptyList())).distinct().takeIf { it.isNotEmpty() }
    )

    open val promptSuffix = """
Parse the code into a structured format that describes its components:
1. Separate the content into sections, paragraphs, statements, etc.
2. All source content should be included in the output, with paraphrasing, corrections, and context as needed
3. Each content leaf node text should be simple and self-contained
4. Assign relevant tags to each node to improve searchability and categorization.
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
        @Description("Hierarchical structure and data") override val content_list: List<CodeContent>? = null,
    ) : ParsingModel.DocumentData

    data class CodeContent(
        @Description("Content type, e.g. function, class, comment") override val type: String = "",
        @Description("Brief, self-contained text either copied, paraphrased, or summarized") override val text: String? = null,
        @Description("Sub-elements") override val content_list: List<CodeContent>? = null,
        @Description("Tags - related topics and non-entity indexing") override val tags: List<String>? = null
    ) : ParsingModel.ContentData

    companion object {
        val log = org.slf4j.LoggerFactory.getLogger(CodeParsingModel::class.java)
    }
}