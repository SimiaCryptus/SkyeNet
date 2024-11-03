package com.simiacryptus.skyenet.apps.plan.knowledge

import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.skyenet.apps.parse.CodeParsingModel
import com.simiacryptus.skyenet.apps.parse.DocumentParserApp
import com.simiacryptus.skyenet.apps.parse.DocumentParsingModel
import com.simiacryptus.skyenet.apps.parse.DocumentRecord.Companion.saveAsBinary
import com.simiacryptus.skyenet.apps.parse.ProgressState
import com.simiacryptus.skyenet.apps.plan.*
import com.simiacryptus.skyenet.util.MarkdownUtil
import com.simiacryptus.skyenet.webui.session.SessionTask
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.util.concurrent.Executors

class KnowledgeIndexingTask(
    planSettings: PlanSettings,
    planTask: KnowledgeIndexingTaskData?
) : AbstractTask<KnowledgeIndexingTask.KnowledgeIndexingTaskData>(planSettings, planTask) {

    class KnowledgeIndexingTaskData(
        @Description("The file paths to process and index")
        val file_paths: List<String>,
        @Description("The type of parsing to use (document, code)")
        val parsing_type: String = "document",
        @Description("The chunk size for parsing (default 0.1)")
        val chunk_size: Double = 0.1,
        task_description: String? = null,
        task_dependencies: List<String>? = null,
        state: TaskState? = null,
    ) : PlanTaskBase(
        task_type = TaskType.KnowledgeIndexing.name,
        task_description = task_description,
        task_dependencies = task_dependencies,
        state = state
    )

    override fun promptSegment() = """
        KnowledgeIndexing - Process and index files for semantic search
        ** Specify the file paths to process
        ** Specify the parsing type (document or code)
        ** Optionally specify the chunk size (default 0.1)
    """.trimIndent()

    override fun run(
        agent: PlanCoordinator,
        messages: List<String>,
        task: SessionTask,
        api: ChatClient,
        resultFn: (String) -> Unit,
        api2: OpenAIClient,
        planSettings: PlanSettings
    ) {
        val filePaths = planTask?.file_paths ?: return
        val files = filePaths.map { File(it) }.filter { it.exists() }
        
        if (files.isEmpty()) {
            val result = "No valid files found to process"
            task.add(MarkdownUtil.renderMarkdown(result, ui = agent.ui))
            resultFn(result)
            return
        }

        val threadPool = Executors.newFixedThreadPool(8)
        try {
            val parsingModel = when (planTask.parsing_type.lowercase()) {
                "code" -> CodeParsingModel(planSettings.defaultModel.chatModel(), planTask.chunk_size)
                else -> DocumentParsingModel(planSettings.defaultModel.chatModel(), planTask.chunk_size)
            }

            val progressState = ProgressState()
            var currentProgress = 0.0
            progressState.onUpdate += {
                val newProgress = it.progress / it.max
                if (newProgress != currentProgress) {
                    currentProgress = newProgress
                    task.add(MarkdownUtil.renderMarkdown("Processing: ${(currentProgress * 100).toInt()}%", ui = agent.ui))
                }
            }

            saveAsBinary<Map<String, Any>>(
                api2,
                threadPool,
                progressState = progressState,
                *files.map { it.absolutePath }.toTypedArray()
            )

            val result = buildString {
                appendLine("# Knowledge Indexing Complete")
                appendLine()
                appendLine("Processed ${files.size} files:")
                files.forEach { file ->
                    appendLine("* ${file.name}")
                }
            }
            task.add(MarkdownUtil.renderMarkdown(result, ui = agent.ui))
            resultFn(result)
        } finally {
            threadPool.shutdown()
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(KnowledgeIndexingTask::class.java)
    }
}