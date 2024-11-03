package com.simiacryptus.skyenet.apps.parse

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.apps.parse.ParsingModel.DocumentData
import com.simiacryptus.skyenet.apps.parse.ProgressState.Companion.progressBar
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.model.User
import com.simiacryptus.skyenet.util.MarkdownUtil
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationSocketManager
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.session.SocketManager
import com.simiacryptus.util.JsonUtil
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Path
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.min

open class DocumentParserApp(
    applicationName: String = "Document Extractor",
    path: String = "/pdfExtractor",
    val api: API = ChatClient(),
    val parsingModel: ParsingModel,
    val reader: (File) -> DocumentReader = {
        when {
            it.name.endsWith(".pdf", ignoreCase = true) -> PDFReader(it)
            else -> TextReader(it)
        }
    },
    val fileInputs: List<Path>? = null,
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
        val app = this
        if (null == (fileInputs ?: settings.fileInputs)) {
            log.info("No file input provided")
        } else (fileInputs ?: settings.fileInputs).apply {
            val progressBar = progressBar(ui.newTask())
            socketManager.pool.submit {
                run(
                    mainTask = ui.newTask(),
                    ui = ui,
                    fileInputs = (app.fileInputs ?: settings.fileInputs?.map { File(it).toPath() } ?: error("File input not provided")),
                    maxPages = settings.maxPages.coerceAtMost(Int.MAX_VALUE),
                    settings = settings,
                    pagesPerBatch = settings.pagesPerBatch,
                    progressBar = progressBar,
                )
            }
        }
        return socketManager
    }

    override fun userMessage(session: Session, user: User?, userMessage: String, ui: ApplicationInterface, api: API) {
        val settings = getSettings(session, user, Settings::class.java) ?: Settings()
        ui.socketManager!!.pool.submit {
            run(
                mainTask = ui.newTask(),
                ui = ui,
                fileInputs = (this.fileInputs ?: settings.fileInputs?.map<String, Path> { File(it).toPath() } ?: error("File input not provided")),
                maxPages = settings.maxPages.coerceAtMost(Int.MAX_VALUE),
                settings = settings,
                pagesPerBatch = settings.pagesPerBatch,
            )
        }
    }

    private fun run(
        mainTask: SessionTask,
        ui: ApplicationInterface,
        fileInputs: List<Path>,
        maxPages: Int,
        settings: Settings,
        pagesPerBatch: Int,
        progressBar: ProgressState? = null
    ) {
        try {
            mainTask.header("PDF Extractor")
            val api = (api as ChatClient).getChildClient().apply {
                val createFile = mainTask.createFile(".logs/api-${UUID.randomUUID()}.log")
                createFile.second?.apply {
                    logStreams += this.outputStream().buffered()
                    mainTask.verbose("API log: <a href=\"file:///$this\">$this</a>")
                }
            }
            val docTabs = TabbedDisplay(mainTask)
            fileInputs.map { it.toFile() }.forEach { file ->
                if (!file.exists()) {
                    mainTask.error(ui, IllegalArgumentException("File not found: $file"))
                    return
                }
                ui.socketManager?.pool?.submit {
                    val docTask = ui.newTask(false).apply { docTabs[file.toString()] = this.placeholder }
                    val pageTabs = TabbedDisplay(docTask)
                    val outputDir = root.resolve("output").apply<File> { mkdirs() }
                    reader(file).use<DocumentReader, Unit> { reader ->
                        var previousPageText = "" // Keep this for context
                        val pageCount = minOf(reader.getPageCount(), maxPages)
                        val pageSets = 0 until pageCount step pagesPerBatch
                        progressBar?.add(0.0, pageCount.toDouble())
                        val futures = pageSets.toList().mapNotNull { batchStart ->
                            val pageTask = ui.newTask(false)
                            val api = api.getChildClient().apply {
                                val createFile = pageTask.createFile(".logs/api-${UUID.randomUUID()}.log")
                                createFile.second?.apply {
                                    logStreams += this.outputStream().buffered()
                                    pageTask.verbose("API log: <a href=\"file:///$this\">$this</a>")
                                }
                            }
                            try {
                                val batchEnd = min(batchStart + pagesPerBatch, pageCount)
                                val text = reader.getText(batchStart, batchEnd)
                                val label = if ((batchStart + 1) != batchEnd) "Pages ${batchStart}-${batchEnd}" else "Page ${batchStart}"
                                val pageTabs = TabbedDisplay(pageTask.apply<SessionTask> { pageTabs[label] = placeholder })
                                if (settings.showImages) {
                                    for (pageIndex in batchStart until batchEnd) {
                                        val image = reader.renderImage(pageIndex, settings.dpi)
                                        ui.newTask(false).apply<SessionTask> {
                                            pageTabs["Image ${1 + (pageIndex - batchStart)}"] = placeholder
                                            image(image)
                                        }
                                        if (settings.saveImageFiles) {
                                            val imageFile =
                                                outputDir.resolve("page_${pageIndex}.${settings.outputFormat.lowercase(Locale.getDefault())}")
                                            when (settings.outputFormat.uppercase(Locale.getDefault())) {
                                                "PNG" -> ImageIO.write(image, "PNG", imageFile)
                                                "JPEG", "JPG" -> ImageIO.write(image, "JPEG", imageFile)
                                                "GIF" -> ImageIO.write(image, "GIF", imageFile)
                                                "BMP" -> ImageIO.write(image, "BMP", imageFile)
                                                else -> throw IllegalArgumentException("Unsupported output format: ${settings.outputFormat}")
                                            }
                                        }
                                    }
                                }
                                if (text.isBlank()) {
                                    pageTask.error(ui, IllegalArgumentException("No text extracted from pages $batchStart to $batchEnd"))
                                    return@mapNotNull null
                                }
                                if (settings.saveTextFiles) {
                                    outputDir.resolve("pages_${batchStart}_to_${batchEnd}_text.txt").writeText(text)
                                }
                                val promptList = mutableListOf<String>()
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
                                previousPageText = text
                                ui.socketManager.pool.submit<DocumentData?> {
                                    try {
                                        val jsonResult = parsingModel.getParser(api)(promptList.toList<String>().joinToString<String>("\n\n"))
                                        if (settings.saveTextFiles) {
                                            val jsonFile = outputDir.resolve("pages_${batchStart}_to_${batchEnd}_content.json")
                                            jsonFile.writeText(JsonUtil.toJson(jsonResult))
                                        }
                                        ui.newTask(false).apply<SessionTask> {
                                            pageTabs["Text"] = placeholder
                                            add(
                                                MarkdownUtil.renderMarkdown(
                                                    "\n```text\n${
                                                        text
                                                    }\n```\n", ui = ui
                                                )
                                            )
                                        }
                                        ui.newTask(false).apply<SessionTask> {
                                            pageTabs["JSON"] = placeholder
                                            add(
                                                MarkdownUtil.renderMarkdown(
                                                    "\n```json\n${
                                                        JsonUtil.toJson(jsonResult)
                                                    }\n```\n", ui = ui
                                                )
                                            )
                                        }
                                        jsonResult
                                    } catch (e: Throwable) {
                                        pageTask.error(ui, e)
                                        null
                                    } finally {
                                        progressBar?.add(1.0, 0.0)
                                        pageTask.complete()
                                    }
                                }
                            } catch (e: Throwable) {
                                pageTask.error(ui, e)
                                null
                            }
                        }.toTypedArray()
                        val finalDocument = futures.mapNotNull { try { it.get() } catch (e : Throwable) {
                            mainTask.error(ui, e)
                            null
                        } }.fold(parsingModel.newDocument())
                        { runningDocument, it -> parsingModel.merge(runningDocument, it) }
                        docTask.add(
                            MarkdownUtil.renderMarkdown(
                                """
                                |## Document JSON
                                |
                                |```json
                                |${JsonUtil.toJson(finalDocument)}
                                |```
                                |
                                |Extracted files are saved in: ${outputDir.absolutePath}
                                """.trimMargin(), ui = ui
                            )
                        )
                        if (settings.saveFinalJson) {
                            val finalJsonFile = file.parentFile.resolve(file.name.reversed().split(delimiters = arrayOf("."), false, 2).joinToString("_").reversed() + ".parsed.json")
                            finalJsonFile.writeText(JsonUtil.toJson(finalDocument))
                            docTask.add(
                                MarkdownUtil.renderMarkdown(
                                    "Final JSON saved to: ${finalJsonFile.absolutePath}",
                                    ui = ui
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            mainTask.error(ui, e)
        }
    }

    data class Settings(
        val dpi: Float = 120f,
        val maxPages: Int = Int.MAX_VALUE,
        val outputFormat: String = "PNG",
        val fileInputs: List<String>? = null,
        val showImages: Boolean = true,
        val pagesPerBatch: Int = 1,
        val saveImageFiles: Boolean = false,
        val saveTextFiles: Boolean = false,
        val saveFinalJson: Boolean = true
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