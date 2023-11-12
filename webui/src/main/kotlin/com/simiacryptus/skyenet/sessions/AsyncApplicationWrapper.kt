package com.simiacryptus.skyenet.sessions

import java.util.concurrent.Semaphore
import java.util.function.Consumer

class AsyncApplicationWrapper(
    val parent: ChatApplicationBase,
    sessionId: String,
    sessionDataStorage: SessionDataStorage = parent.sessionDataStorage
) : PersistentSessionBase(
    sessionId = sessionId,
    sessionDataStorage = sessionDataStorage
) {
    private val playSempaphores = mutableMapOf<String, Semaphore>()
    private val threads = mutableMapOf<String, Thread>()
    private val regenTriggers = mutableMapOf<String, Consumer<Unit>>()
    private val linkTriggers = mutableMapOf<String, Consumer<Unit>>()
    private val txtTriggers = mutableMapOf<String, Consumer<String>>()
    private val session: PersistentSessionBase = this

    override fun run(userMessage: String, socket: MessageWebSocket) {
        val operationID = ChatSession.randomID()
        val sessionDiv = newSessionDiv(operationID, ApplicationBase.spinner)
        val thread = Thread {
            playSempaphores[operationID] = Semaphore(0)
            try {
                parent.processMessage(sessionId, userMessage, session, sessionDiv, socket)
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
            }
        }
        threads[operationID] = thread
        thread.start()
    }

    override fun onCmd(id: String, code: String, socket: MessageWebSocket) {
        if (code == "run") {
            playSempaphores[id]?.release()
        }
        if (code == "cancel") {
            threads[id]?.interrupt()
        }
        if (code == "regen") {
            regenTriggers[id]?.accept(Unit)
        }
        if (code.startsWith("link")) {
            linkTriggers[id]?.accept(Unit)
        }
        if (code.startsWith("userTxt,")) {
            txtTriggers[id]?.accept(code.substring("userTxt,".length))
        }
        super.onCmd(id, code, socket)
    }
}