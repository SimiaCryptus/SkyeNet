package com.simiacryptus.skyenet.webui.session

import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.jopenai.proxy.ValidatedObject
import com.simiacryptus.skyenet.core.actors.CodingActor
import com.simiacryptus.skyenet.webui.util.MarkdownUtil.renderMarkdown
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.util.*

abstract class SessionTask(
  private var buffer: MutableList<StringBuilder> = mutableListOf(),
  private val spinner: String = SessionTask.spinner
) {
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
    html: String
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
    tag: String = "div"
  ) = add(message, showSpinner, tag, "response-header")

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
    @Description("The error to display")
    e: Throwable,
    @Description("Whether to show the spinner for the task (default: false)")
    showSpinner: Boolean = false,
    @Description("The html tag to wrap the message in (default: div)")
    tag: String = "div"
  ) = add(
    when {
      e is ValidatedObject.ValidationError -> renderMarkdown(e.message ?: "")
      e is CodingActor.FailedToImplementException -> renderMarkdown(
        """
        |**Failed to Implement** 
        |
        |${e.message}
        |
        |Prefix:
        |```${e.language?.lowercase() ?: ""}
        |${e.prefix}
        |```
        |
        |Implementation Attempt:
        |```${e.language?.lowercase() ?: ""}
        |${e.code}
        |```
        |
        |""".trimMargin()
      )
      else -> renderMarkdown(
        """
        |**Error `${e.javaClass.name}`**
        |
        |```text
        |${e.message}
        |```
        |""".trimMargin()
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
  ) = append("""<$tag class="$className">$message</$tag>""", false)

  @Description("Displays an image to the task output.")
  fun image(
    @Description("The image to display")
    image: BufferedImage
  ) = add("""<img src="${saveFile("${UUID.randomUUID()}.png", image.toPng())}" />""")

  companion object {
    val log = LoggerFactory.getLogger(SessionTask::class.java)

    const val spinner =
      """<div class="spinner-border" role="status"><span class="sr-only">Loading...</span></div>"""

    private fun BufferedImage.toPng(): ByteArray {
      java.io.ByteArrayOutputStream().use { os ->
        javax.imageio.ImageIO.write(this, "png", os)
        return os.toByteArray()
      }
    }

  }
}
