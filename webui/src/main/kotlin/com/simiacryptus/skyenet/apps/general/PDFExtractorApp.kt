package com.simiacryptus.skyenet.apps.general

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.util.JsonUtil
import com.simiacryptus.skyenet.TabbedDisplay
import com.simiacryptus.skyenet.core.actors.ParsedActor
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationSocketManager
import com.simiacryptus.skyenet.webui.session.SocketManager
import com.simiacryptus.skyenet.webui.util.MarkdownUtil
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import org.intellij.lang.annotations.Language
import java.nio.file.Path
import javax.imageio.ImageIO

class PDFExtractorApp(
    applicationName: String = "PDF Extractor",
    path: String = "/pdfExtractor", val fileInput: Path, val api: API
) : ApplicationServer(
    applicationName = applicationName,
    path = path,
    showMenubar = true
) {

    data class PageData(
        @Description("Page identifier") val id: String? = null,
        @Description("Hierarchical structure and data from the page") val content: List<ContentData> = listOf(),
        @Description("Entities extracted from the page") val entities: Map<String, EntityData> = mapOf()
    )

    data class EntityData(
        val fullName: String? = null,
        @Description("Entity attributes extracted from the page") val properties: Map<String, Any>? = null,
        @Description("Entity relationships extracted from the page") val relations: Map<String, String>? = null,
        @Description("Entity type (e.g., person, organization, location)") val type: String? = null
    )

    data class ContentData(
        @Description("Content type, e.g. heading, paragraph, sentence, list") val type: String = "",
        @Description("Content text OR summary") val text: String? = null,
        @Description("Additional structured data") val data: Map<String, Any>? = null,
        @Description("Sub-elements") val content: List<ContentData>? = null,
        @Description("Linked entities") val entities: List<String>? = null,
        @Description("Tags") val tags: List<String>? = null
    )

    override fun newSession(user: User?, session: Session): SocketManager {
        val socketManager = super.newSession(user, session)
        val ui = (socketManager as ApplicationSocketManager).applicationInterface
        val settings = getSettings(session, user, Settings::class.java) ?: Settings()
        val maxPages = settings.maxPages.coerceAtMost(Int.MAX_VALUE)

        val task = ui.newTask()
        socketManager.pool.submit {
            task.add(MarkdownUtil.renderMarkdown("# PDF Extractor", ui = ui))

            val pdfFile = fileInput.toFile()
            if (!pdfFile.exists() || !pdfFile.isFile || !pdfFile.name.endsWith(".pdf", ignoreCase = true)) {
                task.add(MarkdownUtil.renderMarkdown("Error: Invalid PDF file path. Please provide a valid PDF file path.", ui = ui))
                return@submit
            }

            val outputDir = pdfFile.parentFile.resolve(pdfFile.nameWithoutExtension)
            outputDir.mkdirs()

            PDDocument.load(pdfFile).use { document ->
                val renderer = PDFRenderer(document)
                val stripper = PDFTextStripper()
                var previousPageText = ""
                var previousPageJson: PageData? = null
                task.add(
                    MarkdownUtil.renderMarkdown(
                        """
                    ## Processing PDF: ${pdfFile.name}
                    Total pages: ${document.numberOfPages}
                    """.trimIndent(), ui = ui
                    )
                )
                val tabs = TabbedDisplay(task)
                for (pageIndex in 0 until minOf(document.numberOfPages, maxPages)) {
                    stripper.startPage = pageIndex + 1
                    stripper.endPage = pageIndex + 1
                    val text = stripper.getText(document)

                    val textFile = outputDir.resolve("page_${pageIndex + 1}_text.txt")
                    textFile.writeText(text)

                    val promptList = mutableListOf<String>()
                    if (previousPageJson != null) {
                        promptList.add(
                            "Prior Page JSON:\n\n```json\n${
                                JsonUtil.toJson(
                                    previousPageJson
                                )
                            }\n```\n"
                        )
                    }
                    promptList.add("Prior Page Text: $previousPageText")
                    promptList.add("Current Page: $text")

                    @Language("Markdown") val jsonResult = ParsedActor(
                        resultClass = PageData::class.java, prompt = "", parsingModel = ChatModels.Claude35Sonnet, temperature = 0.1
                    ).getParser(
                        api, promptSuffix = """
                        Parse the text into a grammatical structure that describes the content of the page.
                        Follow these guidelines:
                        1. Break down the content into a detailed hierarchical structure down to the statement level.]
                        2. Identify and link entities (people, organizations, locations, etc.) throughout the content and identify their relationships and attributes.
                        3. For tables, include column headers, row headers, and individual cell data in a structured format. Describe the table's purpose and key findings.
                        4. For lists, maintain the hierarchical structure and numbering/bullet points. Provide context for each list and its items.
                        5. Capture any metadata or document properties if present, including page numbers, sections, chapters, etc.
                        6. Ensure all text strings are concise, self-contained, and human-readable. Use summaries for longer text while preserving key details.
                        Aim for the most comprehensive, detailed, and well-organized representation of the page content possible, breaking down every element into its smallest meaningful components.
                        """.trimIndent()
                    ).apply(
                        promptList.toList().joinToString("\n\n")
                    )
                    val jsonFile = outputDir.resolve("page_${pageIndex + 1}_content.json")
                    jsonFile.writeText(JsonUtil.toJson(jsonResult))
                    val pageTabs = TabbedDisplay(ui.newTask(false).apply {
                        tabs["Page ${pageIndex + 1}"] = this.placeholder
                    })
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
                    previousPageText = text
                    previousPageJson = jsonResult

                    val image = renderer.renderImageWithDPI(pageIndex, settings.dpi)
                    val imageFile = outputDir.resolve("page_${pageIndex + 1}.${settings.outputFormat.toLowerCase()}")
                    when (settings.outputFormat.toUpperCase()) {
                        "PNG" -> ImageIO.write(image, "PNG", imageFile)
                        "JPEG", "JPG" -> ImageIO.write(image, "JPEG", imageFile)
                        "GIF" -> ImageIO.write(image, "GIF", imageFile)
                        "BMP" -> ImageIO.write(image, "BMP", imageFile)
                        else -> throw IllegalArgumentException("Unsupported output format: ${settings.outputFormat}")
                    }
                }
            }
            task.add(MarkdownUtil.renderMarkdown("## Extraction Complete", ui = ui))
            task.add(MarkdownUtil.renderMarkdown("Extracted files are saved in: ${outputDir.absolutePath}", ui = ui))
        }
        return socketManager
    }


    data class Settings(
        val dpi: Float = 300f,
        val maxPages: Int = Int.MAX_VALUE,
        val outputFormat: String = "PNG"  // Supported formats: PNG, JPEG, GIF, BMP
    )

    override val settingsClass: Class<*> get() = Settings::class.java

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> initSettings(session: Session): T? = Settings() as T

}