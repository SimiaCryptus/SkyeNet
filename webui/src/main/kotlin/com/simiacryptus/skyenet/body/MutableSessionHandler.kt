package com.simiacryptus.skyenet.body

class MutableSessionHandler(initialDelegate: SessionInterface?) : SessionInterface {
    private var priorDelegates: MutableList<SessionInterface> = mutableListOf()
    private var currentDelegate: SessionInterface? = initialDelegate

    fun setDelegate(delegate: SessionInterface) {
        if(null != currentDelegate) priorDelegates.add(currentDelegate!!)
        currentDelegate = delegate
        for (socket in sockets) {
            currentDelegate!!.addSocket(socket)
        }
    }

    private val sockets: MutableSet<WebSocketServer.MessageWebSocket> = mutableSetOf()

    override fun removeSocket(socket: WebSocketServer.MessageWebSocket) {
        sockets.remove(socket)
        currentDelegate?.removeSocket(socket)
    }

    override fun addSocket(socket: WebSocketServer.MessageWebSocket) {
        sockets.add(socket)
        currentDelegate?.addSocket(socket)
    }
    override fun getReplay(): List<String> =
        (priorDelegates.flatMap { it.getReplay() } + (currentDelegate?.getReplay() ?: listOf()))

    override fun onWebSocketText(socket: WebSocketServer.MessageWebSocket, message: String) {
        currentDelegate?.onWebSocketText(socket, message)
    }
}