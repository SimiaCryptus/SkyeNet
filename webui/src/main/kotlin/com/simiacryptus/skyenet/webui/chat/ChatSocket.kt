package com.simiacryptus.skyenet.webui.chat

import com.simiacryptus.skyenet.webui.session.SocketManager
import com.simiacryptus.skyenet.webui.session.SocketManagerBase
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketAdapter

class ChatSocket(
  private val sessionState: SocketManager,
) : WebSocketAdapter() {

  val user get() = SocketManagerBase.getUser(session)

  override fun onWebSocketConnect(session: Session) {
    super.onWebSocketConnect(session)
    //log.debug("{} - Socket connected: {}", session, session.remote)
    sessionState.addSocket(this, session)
    sessionState.getReplay().forEach {
      try {
        remote.sendString(it)
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  override fun onWebSocketText(message: String) {
    super.onWebSocketText(message)
    sessionState.onWebSocketText(this, message)
  }

  override fun onWebSocketClose(statusCode: Int, reason: String?) {
    super.onWebSocketClose(statusCode, reason)

    sessionState.removeSocket(this)
  }

  companion object
}


