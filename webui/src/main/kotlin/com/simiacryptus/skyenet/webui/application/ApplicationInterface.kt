package com.simiacryptus.skyenet.webui.application

import com.simiacryptus.skyenet.webui.session.SessionMessage
import com.simiacryptus.skyenet.webui.session.SocketManagerBase
import java.util.function.Consumer

class ApplicationInterface(private val inner: ApplicationSocketManager) {
    fun send(html: String) = inner.send(html)
    fun hrefLink(linkText: String, classname: String = """href-link""", handler: Consumer<Unit>) =
        inner.hrefLink(linkText, classname, handler)

    fun textInput(handler: Consumer<String>): String =
        inner.textInput(handler)

    fun newMessage(
        operationID: String = SocketManagerBase.randomID(),
        spinner: String = ApplicationServer.spinner,
        cancelable: Boolean = false
    ): SessionMessage = inner.newMessage(operationID, spinner, cancelable)

}