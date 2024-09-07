package com.simiacryptus.skyenet.apps.general

import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.application.ApplicationServer
import com.simiacryptus.skyenet.webui.application.ApplicationSocketManager
import com.simiacryptus.skyenet.webui.session.SocketManager
import com.simiacryptus.skyenet.webui.util.MarkdownUtil
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import java.nio.file.Path
import javax.imageio.ImageIO

class PDFExtractorApp(
    applicationName: String = "PDF Extractor",
    path: String = "/pdfExtractor",
    val fileInput: Path
) : ApplicationServer(
    applicationName = applicationName,
    path = path,
    showMenubar = true
) {

    override fun newSession(user: User?, session: Session): SocketManager {
        val socketManager = super.newSession(user, session)
        val ui = (socketManager as ApplicationSocketManager).applicationInterface

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
                task.add(MarkdownUtil.renderMarkdown("## Processing PDF: ${pdfFile.name}", ui = ui))
                task.add(MarkdownUtil.renderMarkdown("Total pages: ${document.numberOfPages}", ui = ui))
                for (pageIndex in 0 until document.numberOfPages) {
                    // Extract text
                    stripper.startPage = pageIndex + 1
                    stripper.endPage = pageIndex + 1
                    val text = stripper.getText(document)
                    val textFile = outputDir.resolve("page_${pageIndex + 1}.txt")
                    textFile.writeText(text)

                    // Extract image
                    val image = renderer.renderImageWithDPI(pageIndex, 300f)
                    val imageFile = outputDir.resolve("page_${pageIndex + 1}.png")
                    ImageIO.write(image, "PNG", imageFile)

                    task.add(MarkdownUtil.renderMarkdown("- Processed page ${pageIndex + 1}", ui = ui))
                }
            }
            task.add(MarkdownUtil.renderMarkdown("## Extraction Complete", ui = ui))
            task.add(MarkdownUtil.renderMarkdown("Extracted files are saved in: ${outputDir.absolutePath}", ui = ui))
        }
        return socketManager
    }


    data class Settings(
        val dpi: Float = 300f
    )

    override val settingsClass: Class<*> get() = Settings::class.java

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> initSettings(session: Session): T? = Settings() as T
}