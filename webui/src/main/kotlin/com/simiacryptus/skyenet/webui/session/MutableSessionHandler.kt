package com.simiacryptus.skyenet.webui.session

import com.simiacryptus.skyenet.webui.chat.ChatSocket

class MutableSessionHandler(initialDelegate: SocketManager?) : SocketManager {
    private var priorDelegates: MutableList<SocketManager> = mutableListOf()
    private var currentDelegate: SocketManager? = initialDelegate

    fun setDelegate(delegate: SocketManager) {
        if(null != currentDelegate) priorDelegates.add(currentDelegate!!)
        currentDelegate = delegate
        for (socket in sockets) {
            currentDelegate!!.addSocket(socket)
        }
    }

    private val sockets: MutableSet<ChatSocket> = mutableSetOf()

    override fun removeSocket(socket: ChatSocket) {
        sockets.remove(socket)
        currentDelegate?.removeSocket(socket)
    }

    override fun addSocket(socket: ChatSocket) {
        sockets.add(socket)
        currentDelegate?.addSocket(socket)
    }
    override fun getReplay(): List<String> =
        (priorDelegates.flatMap { it.getReplay() } + (currentDelegate?.getReplay() ?: listOf()))

    override fun onWebSocketText(socket: ChatSocket, message: String) {
        currentDelegate?.onWebSocketText(socket, message)
    }
}