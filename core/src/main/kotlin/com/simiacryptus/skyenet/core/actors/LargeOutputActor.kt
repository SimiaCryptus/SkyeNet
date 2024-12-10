package com.simiacryptus.skyenet.core.actors

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.models.ApiModel
import com.simiacryptus.jopenai.models.ChatModel
import com.simiacryptus.jopenai.models.OpenAIModels
import com.simiacryptus.jopenai.models.TextModel
import com.simiacryptus.jopenai.util.ClientUtil.toContentList

/**
 * An actor that handles large outputs by using recursive replacement.
 * It instructs the initial LLM call to use ellipsis expressions to manage result size,
 * then recursively expands the result by searching for the pattern and making additional LLM calls.
 */
class LargeOutputActor(
    prompt: String = """
        When generating large responses, please:
        1. Break down the content into logical sections
        2. Use named ellipsis markers like '...sectionName...' to indicate where content needs expansion
        3. Keep each section focused and concise
        4. Use descriptive section names that reflect the content

        ## Example format:
        
        ```markdown
        # Topic Title
        ## Overview
        Here's an overview of the topic ...introduction...
        ## Main Points
        The first important aspect is ...mainPoints...
        ## Technical Details
        For technical details, ...technicalDetails...
        ## Conclusion
        To conclude, ...conclusion...
        ```
        
        Note: Each '...sectionName...' will be expanded in subsequent iterations.
    """.trimIndent(),
    name: String? = null,
    model: TextModel = OpenAIModels.GPT4o,
    temperature: Double = 0.3,
    private val maxIterations: Int = 5,
    private val namedEllipsisPattern: Regex = Regex("""\.\.\.(?<sectionName>[\w\s-]+?)\.\.\.""")
) : BaseActor<List<String>, String>(
    prompt = prompt,
    name = name,
    model = model,
    temperature = temperature
) {

    override fun chatMessages(questions: List<String>): Array<ApiModel.ChatMessage> {
        val systemMessage = ApiModel.ChatMessage(
            role = ApiModel.Role.system,
            content = prompt.toContentList()
        )
        val userMessages = questions.map {
            ApiModel.ChatMessage(
                role = ApiModel.Role.user,
                content = it.toContentList()
            )
        }
        return arrayOf(systemMessage) + userMessages
    }

    override fun respond(input: List<String>, api: API, vararg messages: ApiModel.ChatMessage): String {
        var accumulatedResponse = ""
        var currentMessages = messages.toList()
        var iterations = 0
        var previousContext = ""
        var processedSections = mutableSetOf<String>()

        while (iterations < maxIterations) {
            val response = response(*currentMessages.toTypedArray(), api = api).choices.first().message?.content
                ?: throw RuntimeException("No response from LLM")

            // Replace the ellipsis in the accumulated response with the new content
            if (previousContext.isNotEmpty()) {
                val lastEllipsis = namedEllipsisPattern.find(accumulatedResponse)
                if (lastEllipsis != null) {
                    accumulatedResponse = accumulatedResponse.replaceRange(
                        lastEllipsis.range.first,
                        lastEllipsis.range.last + 1,
                        response.trim()
                    )
                }
            } else {
                accumulatedResponse = response.trim()
            }

            val matches = namedEllipsisPattern.findAll(response)
                .mapNotNull { it.groups["sectionName"]?.value }
                .filter { it !in processedSections }
                .toList()

            if (matches.isNotEmpty()) {
                val nextSection = matches.first()
                processedSections.add(nextSection)

                // Identify the pattern after the ellipsis to continue
                val continuationRequest = """
                    |Previous context:
                    |$accumulatedResponse
                    |
                    |Continue the section '$nextSection' by expanding the ellipsis. 
                    |Make sure the response flows naturally with the existing content.
                    |Keep the response focused and avoid creating new ellipsis markers.
                    """.trimMargin()
                currentMessages = listOf(
                    ApiModel.ChatMessage(
                        role = ApiModel.Role.user,
                        content = continuationRequest.toContentList()
                    )
                )
                previousContext = accumulatedResponse
                iterations++
            } else {
                break
            }
        }

        if (iterations == maxIterations && namedEllipsisPattern.containsMatchIn(accumulatedResponse)) {
            throw RuntimeException("""
                |Maximum iterations ($maxIterations) reached. Output may be incomplete.
                |Processed sections: ${processedSections.joinToString(", ")}
                |Remaining ellipsis markers: ${
                    namedEllipsisPattern.findAll(accumulatedResponse)
                        .mapNotNull { it.groups["sectionName"]?.value }
                        .joinToString(", ")
                }
                |Current length: ${accumulatedResponse.length}
            """.trimMargin())
        }

        return accumulatedResponse
    }

    override fun withModel(model: ChatModel): LargeOutputActor {
        return LargeOutputActor(
            prompt = this.prompt,
            name = this.name,
            model = model,
            temperature = this.temperature,
            maxIterations = this.maxIterations,
            namedEllipsisPattern = this.namedEllipsisPattern
        )
    }
}