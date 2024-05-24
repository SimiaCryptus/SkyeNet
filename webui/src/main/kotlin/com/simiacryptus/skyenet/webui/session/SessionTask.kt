package com.simiacryptus.skyenet.webui.session

import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.proxy.ValidatedObject
import com.simiacryptus.skyenet.core.actors.CodingActor
import com.simiacryptus.skyenet.core.platform.StorageInterface.Companion.long64
import com.simiacryptus.skyenet.webui.application.ApplicationInterface
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage

abstract class SessionTask(
    val operationID: String,
    private var buffer: MutableList<StringBuilder> = mutableListOf(),
    private val spinner: String = SessionTask.spinner
) {

    open fun isInteractive() = true

    val placeholder: String get() = "<div id=\"$operationID\"></div>"

    private val currentText: String
        get() = buffer.filter { it.isNotBlank() }.joinToString("")

    private fun append(
        htmlToAppend: String,
        showSpinner: Boolean
    ): StringBuilder? {
        val stringBuilder: StringBuilder?
        if (htmlToAppend.isNotBlank()) {
            stringBuilder = StringBuilder("<div>$htmlToAppend</div>")
            buffer += stringBuilder
        } else {
            stringBuilder = null
        }
        send(currentText + if (showSpinner) "<div>$spinner</div>" else "")
        return stringBuilder
    }

    protected abstract fun send(
        html: String = currentText
    )

    @Description("Saves the given data to a file and returns the url of the file.")
    abstract fun saveFile(
        @Description("The name of the file to save")
        relativePath: String,
        @Description("The data to save")
        data: ByteArray
    ): String

    @Description("Adds a message to the task output.")
    fun add(
        @Description("The message to add")
        message: String,
        @Description("Whether to show the spinner for the task (default: true)")
        showSpinner: Boolean = true,
        @Description("The html tag to wrap the message in (default: div)")
        tag: String = "div",
        @Description("The css class to apply to the message (default: response-message)")
        className: String = "response-message"
    ) = append("""<$tag class="$className">$message</$tag>""", showSpinner)

    @Description("Adds a hideable message to the task output.")
    fun hideable(
        ui: ApplicationInterface?,
        @Description("The message to add")
        message: String,
        @Description("Whether to show the spinner for the task (default: true)")
        showSpinner: Boolean = true,
        @Description("The html tag to wrap the message in (default: div)")
        tag: String = "div",
        @Description("The css class to apply to the message (default: response-message)")
        className: String = "response-message"
    ): StringBuilder? {
        var windowBuffer: StringBuilder? = null
        val closeButton = """<span class="close">${
            ui?.hrefLink("&times;", "close-button href-link") {
                windowBuffer?.clear()
                send()
            }
        }</span>"""
        windowBuffer = append("""<$tag class="$className">$closeButton$message</$tag>""", showSpinner)
        return windowBuffer
    }

    @Description("Echos a user message to the task output.")
    fun echo(
        @Description("The message to echo")
        message: String,
        @Description("Whether to show the spinner for the task (default: true)")
        showSpinner: Boolean = true,
        @Description("The html tag to wrap the message in (default: div)")
        tag: String = "div"
    ) = add(message, showSpinner, tag, "user-message")

    @Description("Adds a header to the task output.")
    fun header(
        @Description("The message to add")
        message: String,
        @Description("Whether to show the spinner for the task (default: true)")
        showSpinner: Boolean = true,
        @Description("The html tag to wrap the message in (default: div)")
        tag: String = "div",
        classname: String = "response-header"
    ) = add(message, showSpinner, tag, classname)

    @Description("Adds a verbose message to the task output; verbose messages are hidden by default.")
    fun verbose(
        @Description("The message to add")
        message: String,
        @Description("Whether to show the spinner for the task (default: true)")
        showSpinner: Boolean = true,
        @Description("The html tag to wrap the message in (default: pre)")
        tag: String = "pre"
    ) = add(message, showSpinner, tag, "verbose")

    @Description("Displays an error in the task output.")
    fun error(
        ui: ApplicationInterface?,
        @Description("The error to display")
        e: Throwable,
        @Description("Whether to show the spinner for the task (default: false)")
        showSpinner: Boolean = false,
        @Description("The html tag to wrap the message in (default: div)")
        tag: String = "div"
    ) = hideable(
        ui,
        when {
            e is ValidatedObject.ValidationError -> renderMarkdown(
                """
        |**Data Validation Error** 
        |
        |${e.message}
        |
        |Stack Trace:
        |```text
        |${e.stackTraceTxt/*.indent("  ")*/}
        |```
        |
        |""".trimMargin(), ui = ui
            )

            e is CodingActor.FailedToImplementException -> renderMarkdown(
                """
        |**Failed to Implement** 
        |
        |${e.message}
        |
        |Prefix:
        |```${e.language?.lowercase() ?: ""}
        |${/*escapeHtml4*/(e.prefix/*?.indent("  ")*/ ?: "")}
        |```
        |
        |Implementation Attempt:
        |```${e.language?.lowercase() ?: ""}
        |${/*escapeHtml4*/(e.code/*?.indent("  ")*/ ?: "")}
        |```
        |
        |""".trimMargin(), ui = ui
            )

            else -> renderMarkdown(
                """
        |**Error `${e.javaClass.name}`**
        |
        |```text
        |${e.stackTraceToString().let { /*escapeHtml4*/(it)/*.indent("  ")*/ }}
        |```
        |""".trimMargin(), ui = ui
            )
        }, showSpinner, tag, "error"
    )

    @Description("Displays a final message in the task output. This will hide the spinner.")
    fun complete(
        @Description("The message to display")
        message: String = "",
        @Description("The html tag to wrap the message in (default: div)")
        tag: String = "div",
        @Description("The css class to apply to the message (default: response-message)")
        className: String = "response-message"
    ) = append(if (message.isNotBlank()) """<$tag class="$className">$message</$tag>""" else "", false)

    @Description("Displays an image to the task output.")
    fun image(
        @Description("The image to display")
        image: BufferedImage
    ) = add("""<img src="${saveFile("${long64()}.png", image.toPng())}" />""")

    companion object {
        val log = LoggerFactory.getLogger(SessionTask::class.java)

        const val spinner =
            """<div class="spinner-border" role="status"><span class="sr-only">Loading...</span></div>"""

        fun BufferedImage.toPng(): ByteArray {
            java.io.ByteArrayOutputStream().use { os ->
                javax.imageio.ImageIO.write(this, "png", os)
                return os.toByteArray()
            }
        }

    }
}

val Throwable.stackTraceTxt: String
    get() {
        val sw = java.io.StringWriter()
        val pw = java.io.PrintWriter(sw)
        printStackTrace(pw)
        return sw.toString()
    }
