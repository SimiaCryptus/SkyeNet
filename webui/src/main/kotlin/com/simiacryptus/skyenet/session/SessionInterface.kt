package com.simiacryptus.skyenet.session

import com.simiacryptus.skyenet.chat.ChatSocket

interface SessionInterface {
    fun removeSocket(socket: ChatSocket)
    fun addSocket(socket: ChatSocket)
    fun getReplay(): List<String>
    fun onWebSocketText(socket: ChatSocket, message: String)
}