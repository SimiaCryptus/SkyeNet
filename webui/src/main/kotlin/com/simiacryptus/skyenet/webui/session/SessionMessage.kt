package com.simiacryptus.skyenet.webui.session

import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.util.UUID

abstract class SessionMessage(
    private var responseContents: String,
    private val spinner: String = SessionMessage.spinner
) {
    private fun append(htmlToAppend: String, showSpinner: Boolean) {
        if (htmlToAppend.isNotBlank()) {
            responseContents += """<div>$htmlToAppend</div>"""
        }
        return send("$responseContents${if (showSpinner) "<div>$spinner</div>" else ""}")
    }

    abstract fun send(html: String)
    abstract fun save(file: String, data: ByteArray) : String

    fun add(
        message: String,
        showSpinner: Boolean = true,
        tag: String = "div",
        className: String = "response-message"
    ) = append("""<$tag class="$className">$message</$tag>""", showSpinner)

    fun echo(message: String, showSpinner: Boolean = true, tag: String = "div") =
        add(message, showSpinner, tag, "user-message")

    fun header(message: String, showSpinner: Boolean = true, tag: String = "div") =
        add(message, showSpinner, tag, "response-header")

    fun verbose(message: String, showSpinner: Boolean = true, tag: String = "pre") =
        add(message, showSpinner, tag, "verbose")

    fun error(message: String, showSpinner: Boolean = false, tag: String = "div") =
        add(message, showSpinner, tag, "error")

    fun error(e: Throwable, showSpinner: Boolean = false, tag: String = "div") =
        add(
            "Error: ${renderMarkdown(e.message ?: "")}",
            showSpinner, tag, "error"
        )

    fun complete(
        message: String = "",
        tag: String = "div",
        className: String = "response-message"
    ) = append("""<$tag class="$className">$message</$tag>""", false)

    fun image(image: BufferedImage) {
        add("""<img src="${save("${UUID.randomUUID()}.png", image.toPng())}" />""")
    }

    companion object {
        val log = LoggerFactory.getLogger(SessionMessage::class.java)

        val spinner =
            """<div class="spinner-border" role="status"><span class="sr-only">Loading...</span></div>"""

        private fun BufferedImage.toPng(): ByteArray {
            java.io.ByteArrayOutputStream().use { os ->
                javax.imageio.ImageIO.write(this, "png", os)
                return os.toByteArray()
            }
        }

    }
}
