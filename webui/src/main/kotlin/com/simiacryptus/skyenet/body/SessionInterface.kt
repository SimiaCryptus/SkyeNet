package com.simiacryptus.skyenet.body

interface SessionInterface {
    fun removeSocket(socket: WebSocketServer.MessageWebSocket)
    fun addSocket(socket: WebSocketServer.MessageWebSocket)
    fun getReplay(): List<String>
    fun onWebSocketText(socket: WebSocketServer.MessageWebSocket, message: String)
}