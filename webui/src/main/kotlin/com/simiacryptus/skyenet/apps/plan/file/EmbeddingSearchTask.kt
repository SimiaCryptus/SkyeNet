package com.simiacryptus.skyenet.apps.plan.file

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.models.ApiModel
import com.simiacryptus.jopenai.opt.DistanceType
import com.simiacryptus.skyenet.apps.parsers.DocumentRecord
import com.simiacryptus.skyenet.apps.plan.*
import com.simiacryptus.skyenet.util.MarkdownUtil
import com.simiacryptus.skyenet.webui.session.SessionTask
import org.slf4j.LoggerFactory
import java.nio.file.FileSystems
import java.nio.file.Files
import kotlin.streams.asSequence

class EmbeddingSearchTask(
    planSettings: PlanSettings,
    planTask: EmbeddingSearchTaskData?
) : AbstractTask<EmbeddingSearchTask.EmbeddingSearchTaskData>(planSettings, planTask) {
    class EmbeddingSearchTaskData(
        @Description("The search query to look for in the embeddings")
        val search_query: String,
        @Description("The distance type to use for comparing embeddings (Euclidean, Manhattan, or Cosine)")
        val distance_type: DistanceType = DistanceType.Cosine,
        @Description("The number of top results to return")
        val top_k: Int = 5,
        @Description("The specific index files (or file patterns) to be searched")
        val input_files: List<String>? = null,
        task_description: String? = null,
        task_dependencies: List<String>? = null,
        state: TaskState? = null,
    ) : PlanTaskBase(
        task_type = "EmbeddingSearch",
        task_description = task_description,
        task_dependencies = task_dependencies,
        state = state
    )

    override fun promptSegment() = """
EmbeddingSearch - Search for similar embeddings in index files and provide top results
    ** Specify the search query
    ** Specify the distance type (Euclidean, Manhattan, or Cosine)
    ** Specify the number of top results to return
    ** List input index files or file patterns to be searched
    """.trimMargin()

    override fun run(
        agent: PlanCoordinator,
        taskId: String,
        userMessage: String,
        plan: Map<String, PlanTaskBase>,
        planProcessingState: PlanProcessingState,
        task: SessionTask,
        api: API,
        resultFn: (String) -> Unit
    ) {
        val searchResults = performEmbeddingSearch(api as OpenAIClient)
        val formattedResults = formatSearchResults(searchResults)
        task.add(MarkdownUtil.renderMarkdown(formattedResults, ui = agent.ui))
        resultFn(formattedResults)
    }

    private fun performEmbeddingSearch(api: OpenAIClient): List<EmbeddingSearchResult> {
        val queryEmbedding = api.createEmbedding(ApiModel.EmbeddingRequest(
            input = planTask?.search_query ?: "",
            model = (planSettings.getTaskSettings(TaskType.EmbeddingSearch).model
                ?: planSettings.defaultModel).modelName

        )).data[0].embedding
        val distanceType = planTask?.distance_type ?: DistanceType.Cosine

        return (planTask?.input_files ?: listOf())
            .flatMap { filePattern ->
                val matcher = FileSystems.getDefault().getPathMatcher("glob:$filePattern")
                Files.walk(root).asSequence()
                    .filter { path ->
                        matcher.matches(root.relativize(path)) && path.toString().endsWith(".index.data")
                    }
                    .flatMap { path ->
                        val records = DocumentRecord.readBinary(path.toString())
                        records.mapNotNull { record ->
                            record.vector?.let { vector ->
                                EmbeddingSearchResult(
                                    file = root.relativize(path).toString(),
                                    record = record,
                                    distance = distanceType.distance(vector, queryEmbedding ?: DoubleArray(0))
                                )
                            }
                        }
                    }
                    .toList()
            }
            .sortedBy { it.distance }
            .take(planTask?.top_k ?: 5)
    }

    private fun formatSearchResults(results: List<EmbeddingSearchResult>): String {
        return buildString {
            appendLine("# Embedding Search Results")
            appendLine()
            results.forEachIndexed { index, result ->
                appendLine("## Result ${index + 1}")
                appendLine("- File: ${result.file}")
                appendLine("- Distance: ${result.distance}")
                appendLine("- Text: ${result.record.text}")
                appendLine("- Type: ${result.record.type}")
                appendLine("- Source Path: ${result.record.sourcePath}")
                appendLine("- JSON Path: ${result.record.jsonPath}")
                appendLine()
            }
        }
    }

    data class EmbeddingSearchResult(
        val file: String,
        val record: DocumentRecord,
        val distance: Double
    )

    companion object {
        private val log = LoggerFactory.getLogger(EmbeddingSearchTask::class.java)
    }
}