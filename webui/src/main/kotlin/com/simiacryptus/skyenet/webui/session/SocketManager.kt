package com.simiacryptus.skyenet.webui.session

import com.simiacryptus.skyenet.webui.chat.ChatSocket

interface SocketManager {
    fun removeSocket(socket: ChatSocket)
    fun addSocket(socket: ChatSocket)
    fun getReplay(): List<String>
    fun onWebSocketText(socket: ChatSocket, message: String)
}