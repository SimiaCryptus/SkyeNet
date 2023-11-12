package com.simiacryptus.skyenet.sessions

interface SessionInterface {
    fun removeSocket(socket: MessageWebSocket)
    fun addSocket(socket: MessageWebSocket)
    fun getReplay(): List<String>
    fun onWebSocketText(socket: MessageWebSocket, message: String)
}