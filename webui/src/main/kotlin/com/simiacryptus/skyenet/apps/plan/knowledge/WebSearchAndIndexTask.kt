package com.simiacryptus.skyenet.apps.plan.knowledge

import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.skyenet.apps.parse.DocumentRecord
import com.simiacryptus.skyenet.apps.plan.AbstractTask
import com.simiacryptus.skyenet.apps.plan.PlanCoordinator
import com.simiacryptus.skyenet.apps.plan.PlanSettings
import com.simiacryptus.skyenet.apps.plan.PlanTaskBase
import com.simiacryptus.skyenet.webui.session.SessionTask
import org.apache.commons.io.FileUtils
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.Executors
import kotlin.collections.forEachIndexed
import kotlin.collections.map
import kotlin.collections.mapNotNull
import kotlin.io.nameWithoutExtension
import kotlin.jvm.java
import kotlin.text.appendLine
import kotlin.text.replace
import kotlin.text.take
import kotlin.text.trimMargin
import kotlin.to

class WebSearchAndIndexTask(
    planSettings: PlanSettings,
    planTask: WebSearchAndIndexTaskData?
) : AbstractTask<WebSearchAndIndexTask.WebSearchAndIndexTaskData>(planSettings, planTask) {

    class WebSearchAndIndexTaskData(
        @Description("The search query to use for web search")
        val search_query: String,
        @Description("The number of search results to process (max 10)")
        val num_results: Int = 5,
        @Description("The directory to store downloaded and indexed content")
        val output_directory: String,
        task_description: String? = null,
        task_dependencies: List<String>? = null,
        state: TaskState? = null,
    ) : PlanTaskBase(
        task_type = "WebSearchAndIndex",
        task_description = task_description,
        task_dependencies = task_dependencies,
        state = state
    )

    override fun promptSegment() = """
        WebSearchAndIndex - Search web, download content, parse and index for future embedding search
        ** Specify the search query
        ** Specify number of results to process (max 10)
        ** Specify output directory for indexed content
    """.trimMargin()

    override fun run(
        agent: PlanCoordinator,
        messages: List<String>,
        task: SessionTask,
        api: ChatClient,
        resultFn: (String) -> Unit,
        api2: OpenAIClient,
        planSettings: PlanSettings
    ) {
        val searchResults = performGoogleSearch(planSettings)
        val downloadedFiles = downloadAndSaveContent(searchResults)
        val indexedFiles = indexContent(downloadedFiles, api2)
        
        val summary = buildString {
            appendLine("# Web Search and Index Results")
            appendLine("## Search Query: ${planTask?.search_query}")
            appendLine("## Downloaded and Indexed Files:")
            indexedFiles.forEachIndexed { index, file ->
                appendLine("${index + 1}. ${file.name}")
            }
        }
        
        resultFn(summary)
    }

    private fun performGoogleSearch(planSettings: PlanSettings): List<Map<String, Any>> {
        val client = HttpClient.newBuilder().build()
        val encodedQuery = URLEncoder.encode(planTask?.search_query, "UTF-8")
        val uriBuilder = "https://www.googleapis.com/customsearch/v1?key=${planSettings.googleApiKey}&cx=${planSettings.googleSearchEngineId}&q=$encodedQuery&num=${planTask?.num_results}"
        
        val request = HttpRequest.newBuilder()
            .uri(URI.create(uriBuilder))
            .GET()
            .build()
            
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw RuntimeException("Google API request failed with status ${response.statusCode()}: ${response.body()}")
        }
        
        val searchResults: Map<String, Any> = JsonUtil.fromJson(response.body(), Map::class.java)
        return (searchResults["items"] as List<Map<String, Any>>?) ?: emptyList()
    }

    private fun downloadAndSaveContent(searchResults: List<Map<String, Any>>): List<File> {
        val outputDir = File(planTask?.output_directory ?: "web_content")
        outputDir.mkdirs()
        
        val client = HttpClient.newBuilder().build()
        return searchResults.mapNotNull { result ->
            try {
                val url = result["link"] as String
                val title = result["title"] as String
                val fileName = "${sanitizeFileName(title)}.html"
                val outputFile = File(outputDir, fileName)
                
                val request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build()
                    
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() == 200) {
                    FileUtils.writeStringToFile(outputFile, response.body(), "UTF-8")
                    outputFile
                } else null
            } catch (e: Exception) {
                log.error("Error downloading content", e)
                null
            }
        }
    }

    private fun indexContent(files: List<File>, api: OpenAIClient): List<File> {
        val threadPool = Executors.newFixedThreadPool(8)
        try {
            val indexedFiles = files.map { file ->
                val content = FileUtils.readFileToString(file, "UTF-8")
                val cleanContent = Jsoup.parse(content).text()
                
                DocumentRecord(
                    text = cleanContent,
                    sourcePath = file.absolutePath,
                    metadata = mapOf(
                        "title" to file.nameWithoutExtension,
                        "type" to "web_content"
                    )
                )
            }
            
            // Save as binary index
            val indexFile = File(planTask?.output_directory, "web_content.index.data")
            DocumentRecord.saveAsBinary(indexFile.absolutePath, indexedFiles)
            
            return listOf(indexFile)
        } finally {
            threadPool.shutdown()
        }
    }

    private fun sanitizeFileName(fileName: String): String {
        return fileName.replace(Regex("[^a-zA-Z0-9.-]"), "_").take(50)
    }

    companion object {
        private val log = LoggerFactory.getLogger(WebSearchAndIndexTask::class.java)
    }
}