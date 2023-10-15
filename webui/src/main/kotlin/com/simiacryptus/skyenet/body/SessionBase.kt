package com.simiacryptus.skyenet.body

abstract class SessionBase(val sessionId: String) : SessionInterface {
    private val sockets: MutableSet<WebSocketServer.MessageWebSocket> = mutableSetOf()

    override fun removeSocket(socket: WebSocketServer.MessageWebSocket) {
        sockets.remove(socket)
    }

    override fun addSocket(socket: WebSocketServer.MessageWebSocket) {
        sockets.add(socket)
    }
    protected fun publish(
        out: String,
    ) {
        val socketsSnapshot = sockets.toTypedArray()
        socketsSnapshot.forEach {
            try {
                it.remote.sendString(out)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    private val sentMessages: MutableList<String> = mutableListOf()

    protected open fun send(out: String) {
        sentMessages.add(out)
        publish(out)
    }

    override fun getReplay(): List<String> {
        return sentMessages
    }

    fun newSessionDiv(
        operationID: String, spinner: String
    ): SessionDiv {
        var responseContents = ChatSession.divInitializer(operationID)
        send(responseContents)
        return object : SessionDiv() {
            override fun append(htmlToAppend: String, showSpinner: Boolean) {
                if (htmlToAppend.isNotBlank()) {
                    responseContents += """<div>$htmlToAppend</div>"""
                }
                val spinner1 = if (showSpinner) """<div>$spinner</div>""" else ""
                return this@SessionBase.send("""$responseContents$spinner1""")
            }
        }
    }
}