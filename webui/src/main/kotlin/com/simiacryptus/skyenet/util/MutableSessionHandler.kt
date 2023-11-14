package com.simiacryptus.skyenet.util

import com.simiacryptus.skyenet.chat.ChatSocket
import com.simiacryptus.skyenet.session.SessionInterface

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