package com.simiacryptus.skyenet.apps.general

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.apps.general.parsers.ParsingModel
import com.simiacryptus.skyenet.apps.general.parsers.DefaultParsingModel
import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.apps.general.parsers.PDFReader
import com.simiacryptus.skyenet.apps.general.parsers.TextReader
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationSocketManager
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.session.SocketManager
import com.simiacryptus.skyenet.webui.util.MarkdownUtil
import org.intellij.lang.annotations.Language
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Path
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.min

class DocumentParserApp(
    applicationName: String = "Document Extractor",
    path: String = "/pdfExtractor",
    val api: API = ChatClient(),
    val parsingModel: ParsingModel = DefaultParsingModel(ChatModels.Claude35Sonnet, 0.1),
    val reader: (File) -> DocumentReader = {
        when {
            it.name.endsWith(".pdf", ignoreCase = true) -> PDFReader(it)
            it.name.endsWith(".txt", ignoreCase = true) -> TextReader(it)
            it.name.endsWith(".md", ignoreCase = true) -> TextReader(it)
            it.name.endsWith(".html", ignoreCase = true) -> TextReader(it)
            else -> throw IllegalArgumentException("Unsupported file type")
        }
    },
    val fileInput: Path? = null,
) : ApplicationServer(
    applicationName = applicationName,
    path = path,
    showMenubar = true
) {
    override val singleInput: Boolean = true
    override val stickyInput: Boolean = false

    override fun newSession(user: User?, session: Session): SocketManager {
        val socketManager = super.newSession(user, session)
        val ui = (socketManager as ApplicationSocketManager).applicationInterface
        val settings = getSettings(session, user, Settings::class.java) ?: Settings()
        if (null == (fileInput ?: settings.fileInput)) {
            log.info("No file input provided")
        } else socketManager.pool.submit {
            run(
                task = ui.newTask(),
                ui = ui,
                fileInput = (this.fileInput ?: settings.fileInput?.let { File(it).toPath() }
                ?: error("File input not provided")).apply {
                    if (!toFile().exists()) error("File not found: $this")
                },
                maxPages = settings.maxPages.coerceAtMost(Int.MAX_VALUE),
                settings = settings,
                pagesPerBatch = settings.pagesPerBatch,
                root = dataStorage.getDataDir(user, session)
            )
        }
        return socketManager
    }

    override fun userMessage(session: Session, user: User?, userMessage: String, ui: ApplicationInterface, api: API) {
        val settings = getSettings(session, user, Settings::class.java) ?: Settings()
        val fileInput =
            (fileInput ?: settings.fileInput?.let { File(it).toPath() } ?: error("File input not provided")).apply {
                if (!toFile().exists()) error("File not found: $this")
            }
        ui.socketManager!!.pool.submit {
            run(
                task = ui.newTask(),
                ui = ui,
                fileInput = fileInput,
                maxPages = settings.maxPages.coerceAtMost(Int.MAX_VALUE),
                settings = settings,
                pagesPerBatch = settings.pagesPerBatch,
                root = dataStorage.getDataDir(user, session)
            )
        }
    }

    private fun run(
        task: SessionTask,
        ui: ApplicationInterface,
        fileInput: Path,
        maxPages: Int,
        settings: Settings,
        pagesPerBatch: Int,
        root: File
    ) {
        try {
            val pdfFile = fileInput.toFile()
            if (!pdfFile.exists() || !pdfFile.isFile || !pdfFile.name.endsWith(".pdf", ignoreCase = true)) {
                throw IllegalArgumentException("Invalid PDF file: $pdfFile")
            }
            task.add(MarkdownUtil.renderMarkdown("# PDF Extractor", ui = ui))
            val outputDir = root.resolve("output").apply { mkdirs() }
            lateinit var runningDocument: ParsingModel.DocumentData
            reader(pdfFile).use { reader ->
                runningDocument = parsingModel.newDocument()
                var previousPageText = "" // Keep this for context
                task.add(
                    MarkdownUtil.renderMarkdown(
                        """
                        ## Processing PDF: ${pdfFile.name}
                        Total pages: ${reader.getPageCount()}
                        """.trimIndent(), ui = ui
                    )
                )
                val pageCount = minOf(reader.getPageCount(), maxPages)
                val tabs = TabbedDisplay(task)
                for (batchStart in 0 until pageCount step pagesPerBatch) {
                    val batchEnd = min(batchStart + pagesPerBatch, pageCount)
                    val pageTask = ui.newTask(false)
                    val pageTabs = TabbedDisplay(pageTask.apply {
                        val label =
                            if ((batchStart + 1) != batchEnd) "Pages ${batchStart + 1}-${batchEnd}" else "Page ${batchStart + 1}"
                        tabs[label] = this.placeholder
                    })
                    try {
                        val text = reader.getText(batchStart + 1, batchEnd)
                        outputDir.resolve("pages_${batchStart + 1}_to_${batchEnd}_text.txt").writeText(text)
                        val promptList = mutableListOf<String>()
                        promptList.add(
                            """
                            |# Accumulated Prior JSON:
                            |
                            |FOR INFORMATIVE CONTEXT ONLY. DO NOT COPY TO OUTPUT.
                            |```json
                            |${JsonUtil.toJson(runningDocument)}
                            |```
                            """.trimMargin()
                        )
                        promptList.add(
                            """
                            |# Prior Text
                            |
                            |FOR INFORMATIVE CONTEXT ONLY. DO NOT COPY TO OUTPUT.
                            |```text
                            |$previousPageText
                            |```
                            |""".trimMargin()
                        )
                        promptList.add(
                            """
                            |# Current Page
                            |
                            |```text
                            |$text
                            |```
                            """.trimMargin()
                        )
                        @Language("Markdown") val jsonResult = parsingModel.getParser(api).let {
                            it(promptList.toList().joinToString("\n\n"))
                        }
                        val jsonFile = outputDir.resolve("pages_${batchStart + 1}_to_${batchEnd}_content.json")
                        jsonFile.writeText(JsonUtil.toJson(jsonResult))
                        ui.newTask(false).apply {
                            pageTabs["Text"] = this.placeholder
                            add(
                                MarkdownUtil.renderMarkdown(
                                    "\n```text\n${
                                        text
                                    }\n```\n", ui = ui
                                )
                            )
                        }
                        ui.newTask(false).apply {
                            pageTabs["JSON"] = this.placeholder
                            add(
                                MarkdownUtil.renderMarkdown(
                                    "\n```json\n${
                                        JsonUtil.toJson(jsonResult)
                                    }\n```\n", ui = ui
                                )
                            )
                        }
                        for (pageIndex in batchStart until batchEnd) {
                            val image = reader.renderImage(pageIndex, settings.dpi)
                            if (settings.showImages) {
                                ui.newTask(false).apply {
                                    pageTabs["Image ${pageIndex + 1}"] = this.placeholder
                                    image(image)
                                }
                            }
                            val imageFile =
                                outputDir.resolve("page_${pageIndex + 1}.${settings.outputFormat.lowercase(Locale.getDefault())}")
                            when (settings.outputFormat.uppercase(Locale.getDefault())) {
                                "PNG" -> ImageIO.write(image, "PNG", imageFile)
                                "JPEG", "JPG" -> ImageIO.write(image, "JPEG", imageFile)
                                "GIF" -> ImageIO.write(image, "GIF", imageFile)
                                "BMP" -> ImageIO.write(image, "BMP", imageFile)
                                else -> throw IllegalArgumentException("Unsupported output format: ${settings.outputFormat}")
                            }
                        }
                        runningDocument = parsingModel.merge(runningDocument, jsonResult)
                        ui.newTask(false).apply {
                            pageTabs["Accumulator"] = this.placeholder
                            add(
                                MarkdownUtil.renderMarkdown(
                                    """
                                    |## Accumulated Document JSON
                                    |
                                    |```json
                                    |${JsonUtil.toJson(runningDocument)}
                                    |```
                                    """.trimMargin(), ui = ui
                                )
                            )
                        }
                        previousPageText = text.takeLast(1000)
                    } catch (e: Throwable) {
                        pageTask.error(ui, e)
                        continue
                    }
                }
                task.add(
                    MarkdownUtil.renderMarkdown(
                        """
                        |## Document JSON
                        |
                        |```json
                        |${JsonUtil.toJson(runningDocument)}
                        |```
                        |
                        |Extracted files are saved in: ${outputDir.absolutePath}
                        """.trimMargin(), ui = ui
                    )
                )
            }
        } catch (e: Throwable) {
            task.error(ui, e)
        }
    }

    data class Settings(
        val dpi: Float = 120f,
        val maxPages: Int = Int.MAX_VALUE,
        val outputFormat: String = "PNG",
        val fileInput: String? = "",
        val showImages: Boolean = true,
        val pagesPerBatch: Int = 1
    )

    override val settingsClass: Class<*> get() = Settings::class.java

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> initSettings(session: Session): T = Settings() as T

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(DocumentParserApp::class.java)
    }

    interface DocumentReader : AutoCloseable {
        fun getPageCount(): Int
        fun getText(startPage: Int, endPage: Int): String
        fun renderImage(pageIndex: Int, dpi: Float): BufferedImage
    }

}