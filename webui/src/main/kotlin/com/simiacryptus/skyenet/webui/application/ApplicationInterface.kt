package com.simiacryptus.skyenet.webui.application

import com.simiacryptus.jopenai.describe.Description
import com.simiacryptus.skyenet.webui.session.SessionTask
import java.util.function.Consumer

open class ApplicationInterface(private val inner: ApplicationSocketManager) {
  @Description("Returns html for a link that will trigger the given handler when clicked.")
  open fun hrefLink(
    @Description("The text to display in the link")
    linkText: String,
    @Description("The css class to apply to the link")
    classname: String = """href-link""",
    @Description("The handler to trigger when the link is clicked")
    handler: Consumer<Unit>
  ) = inner.hrefLink(linkText, classname, handler)

  @Description("Returns html for a text input form that will trigger the given handler when submitted.")
  open fun textInput(
    @Description("The handler to trigger when the form is submitted")
    handler: Consumer<String>
  ): String = inner.textInput(handler)

  @Description("Creates a new 'task' that can be used to display the progress of a long-running operation.")
  open fun newTask(
    //cancelable: Boolean = false
  ): SessionTask = inner.newTask(false)

}