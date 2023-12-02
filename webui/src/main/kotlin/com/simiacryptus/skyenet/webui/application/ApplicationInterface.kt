package com.simiacryptus.skyenet.webui.application

import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.skyenet.webui.session.SessionTask
import java.util.function.Consumer

class ApplicationInterface(private val inner: ApplicationSocketManager) {
  @Description("Returns html for a link that will trigger the given handler when clicked.")
  fun hrefLink(
    linkText: String,
    classname: String = """href-link""",
    handler: Consumer<Unit>
  ) = inner.hrefLink(linkText, classname, handler)

  @Description("Returns html for a text input form that will trigger the given handler when submitted.")
  fun textInput(
    handler: Consumer<String>
  ): String = inner.textInput(handler)

  fun newTask(
    //cancelable: Boolean = false
  ): SessionTask = inner.newTask(false)

}