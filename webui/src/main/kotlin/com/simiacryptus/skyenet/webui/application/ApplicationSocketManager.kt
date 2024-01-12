package com.simiacryptus.skyenet.webui.application

import com.simiacryptus.jopenai.API
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.Session
import com.simiacryptus.skyenet.core.platform.StorageInterface
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.webui.chat.ChatSocket
import com.simiacryptus.skyenet.webui.session.SessionTask
import com.simiacryptus.skyenet.webui.session.SocketManagerBase

abstract class ApplicationSocketManager(
  session: Session,
  owner: User?,
  dataStorage: StorageInterface?,
  applicationClass: Class<*>,
) : SocketManagerBase(
  session = session,
  dataStorage = dataStorage,
  owner = owner,
  applicationClass = applicationClass,
) {
  override fun onRun(userMessage: String, socket: ChatSocket) {
    userMessage(
      session = session,
      user = socket.user,
      userMessage = userMessage,
      socketManager = this,
      api = ApplicationServices.clientManager.getClient(
        session,
        socket.user,
        dataStorage ?: throw IllegalStateException("No data storage")
      )
    )
  }

  open val applicationInterface by lazy { ApplicationInterface(this) }


  abstract fun userMessage(
    session: Session,
    user: User?,
    userMessage: String,
    socketManager: ApplicationSocketManager,
    api: API
  )

  companion object {
    val spinner: String get() = """<div>${SessionTask.spinner}</div>"""
//        val playButton: String get() = """<button class="play-button" data-id="$operationID">▶</button>"""
//        val cancelButton: String get() = """<button class="cancel-button" data-id="$operationID">&times;</button>"""
//        val regenButton: String get() = """<button class="regen-button" data-id="$operationID">♲</button>"""
  }
}