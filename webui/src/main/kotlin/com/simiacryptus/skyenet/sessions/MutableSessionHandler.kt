package com.simiacryptus.skyenet.sessions

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

    private val sockets: MutableSet<MessageWebSocket> = mutableSetOf()

    override fun removeSocket(socket: MessageWebSocket) {
        sockets.remove(socket)
        currentDelegate?.removeSocket(socket)
    }

    override fun addSocket(socket: MessageWebSocket) {
        sockets.add(socket)
        currentDelegate?.addSocket(socket)
    }
    override fun getReplay(): List<String> =
        (priorDelegates.flatMap { it.getReplay() } + (currentDelegate?.getReplay() ?: listOf()))

    override fun onWebSocketText(socket: MessageWebSocket, message: String) {
        currentDelegate?.onWebSocketText(socket, message)
    }
}