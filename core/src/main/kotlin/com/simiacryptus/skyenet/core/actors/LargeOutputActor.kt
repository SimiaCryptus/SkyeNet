package com.simiacryptus.skyenet.core.actors

import com.google.common.base.Strings.commonPrefix
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.models.ApiModel
import com.simiacryptus.jopenai.models.ApiModel.Role
import com.simiacryptus.jopenai.models.ChatModel
import com.simiacryptus.jopenai.models.OpenAIModels
import com.simiacryptus.jopenai.models.TextModel
import com.simiacryptus.jopenai.util.ClientUtil.toChatMessage
import com.simiacryptus.jopenai.util.ClientUtil.toContentList

/**
 * An actor that handles large outputs by using recursive replacement.
 * It instructs the initial LLM call to use ellipsis expressions to manage result size,
 * then recursively expands the result by searching for the pattern and making additional LLM calls.
 */
class LargeOutputActor(
  prompt: String = """
        You are a long-form content writer. You have been tasked with writing a comprehensive guide on a topic.
        1. Break down the content into logical sections using markdown formatting and headers.
        2. To support large content generation, use markers to indicate where content needs expansion.
        3. Expansion markers should use a line formatted like '...sectionName...' to indicate where detailed content should be inserted.
        4. Use descriptive and unique section names that reflect the content expected in that section.
        5. For the initial iteration, provide a high level document structure with a few expansion markers. Each '...sectionName...' will be expanded in subsequent iterations.
    """.trimIndent(),
  name: String? = null,
  model: TextModel = OpenAIModels.GPT4o,
  temperature: Double = 0.3, private val maxIterations: Int = 3, private val namedEllipsisPattern: Regex = Regex("""\.\.\.(?<sectionName>[\w\s-_]+?)\.\.\.""")
) : BaseActor<List<String>, String>(
  prompt = prompt, name = name, model = model, temperature = temperature
) {

  override fun chatMessages(questions: List<String>): Array<ApiModel.ChatMessage> {
    val systemMessage = ApiModel.ChatMessage(
      role = Role.system, content = prompt.toContentList()
    )
    val userMessages = questions.map {
      ApiModel.ChatMessage(
        role = Role.user, content = it.toContentList()
      )
    }
    return arrayOf(systemMessage) + userMessages
  }

  override fun respond(input: List<String>, api: API, vararg messages: ApiModel.ChatMessage): String {
    var accumulatedResponse = ""
    var iterations = 0
    while (iterations < maxIterations) {
      if (accumulatedResponse.isEmpty()) {
        accumulatedResponse = response(*messages, api = api).choices.first().message?.content?.trim() ?: throw RuntimeException("No response from LLM")
      }
      val matches = namedEllipsisPattern.findAll(accumulatedResponse).toMutableList()
      if (matches.isEmpty()) break
      val pairs = matches.mapNotNull { matchResult ->
        val nextSection = matchResult.groups["sectionName"]?.value ?: return@mapNotNull null
        val contextLines = 100
        val contextChars = 10000
        Pair(
          matchResult, response(
            *(listOf(
              """
              You are a long-form content writer. You have been tasked with writing a comprehensive guide on a topic by filling in a detail section.
              1. Break down the content into logical sections using markdown formatting and headers.
              2. To support large content generation, use markers to indicate where content needs expansion.
              3. Expansion markers should use a line formatted like '...sectionName...' to indicate where detailed content should be inserted.
              4. Use descriptive and unique section names that reflect the content expected in that section.
              """.trimIndent().toChatMessage(Role.system)
            ) + messages.toList().drop(1) + listOf(
              ApiModel.ChatMessage(
                role = Role.user, content = ("""
                  Previous context:
                  
                  ```
                  """.trimIndent() + accumulatedResponse.substring(0, matchResult.range.first).lines().takeLast(contextLines).joinToString { "  $it" }.takeLast(contextChars) + """
                  ```
                  
                  Continue the section '""".trimIndent() + nextSection + """'
                  Make sure the response flows naturally with the existing content.
                  It should end so that it matches the next section, provided below:
                  
                  ```
                  """.trimIndent() + accumulatedResponse.substring(matchResult.range.last).lines().take(contextLines).joinToString { "  $it" }.take(contextChars) + """
                  ```
                  """.trimIndent()).toContentList()
              )
            )).toTypedArray(), api = api
          )
        )
      }
      accumulatedResponse = pairs.reversed().fold(accumulatedResponse) { acc, (match, response) ->
        val original = response.choices.first().message?.content?.trim() ?: ""
        var replacement = original
        if (replacement.isEmpty()) return acc
        //val replaced = acc.substring(match.range)
        if (replacement.startsWith("```")) {
          replacement = replacement.lines().drop(1).reversed().dropWhile { !it.startsWith("```") }.drop(1).reversed().joinToString("\n")
        }
        val prefix = acc.substring(0, match.range.first)
        val suffix = acc.substring(match.range.last)
        val commonPrefix = commonPrefix(prefix, replacement)
        if (commonPrefix.isNotBlank() && commonPrefix.contains('\n')) replacement = replacement.substring(commonPrefix.length)
        val largestCommonSubstring = largestCommonSubstring(replacement, suffix)
        if (largestCommonSubstring.isNotBlank()) replacement = replacement.substring(0, replacement.indexOf(largestCommonSubstring))
        val replaceRange = acc.replaceRange(match.range, replacement)
        replaceRange
      }
      iterations++
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

fun largestCommonSubstring(a: String, b: String): String {
  val lengths = Array(a.length + 1) { IntArray(b.length + 1) }
  var z = 0
  var ret = ""
  for (i in 0 until a.length) {
    for (j in 0 until b.length) {
      if (a[i] == b[j]) {
        lengths[i + 1][j + 1] = lengths[i][j] + 1
        val len = lengths[i + 1][j + 1]
        if (len > z) {
          z = len
          ret = a.substring(i - z + 1, i + 1)
        }
      }
    }
  }
  return ret
}