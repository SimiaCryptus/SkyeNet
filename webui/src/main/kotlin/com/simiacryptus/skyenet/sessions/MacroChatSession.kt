package com.simiacryptus.skyenet.sessions

import java.util.concurrent.Semaphore
import java.util.function.Consumer

class MacroChatSession(
    val parent: MacroChat,
    sessionId: String,
    sessionDataStorage: SessionDataStorage = parent.sessionDataStorage
) : PersistentSessionBase(
    sessionId = sessionId,
    sessionDataStorage = sessionDataStorage
) {
    private val playSempaphores = mutableMapOf<String, Semaphore>()
    private val threads = mutableMapOf<String, Thread>()
    private val regenTriggers = mutableMapOf<String, Consumer<Unit>>()
    val linkTriggers = mutableMapOf<String, Consumer<Unit>>()
    val txtTriggers = mutableMapOf<String, Consumer<String>>()
    val session: PersistentSessionBase = this
    override fun run(userMessage: String, socket: MessageWebSocket) {
        val operationID = ChatSession.randomID()
        val sessionDiv = newSessionDiv(operationID, ApplicationBase.spinner)
        val thread = Thread {
            playSempaphores[operationID] = Semaphore(0)
            try {
                parent.processMessage(sessionId, userMessage, session, SessionImpl(operationID), sessionDiv, socket)
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
            }
        }
        threads[operationID] = thread
        thread.start()
    }

    inner class SessionImpl(private val operationID: String) : MacroChat.SessionUI {
        override val spinner: String get() = """<div>${ApplicationBase.spinner}</div>"""
        override val playButton: String get() = """<button class="play-button" data-id="$operationID">▶</button>"""
        override val cancelButton: String get() = """<button class="cancel-button" data-id="$operationID">&times;</button>"""
        override val regenButton: String get() = """<button class="regen-button" data-id="$operationID">♲</button>"""

        override fun hrefLink(handler: Consumer<Unit>): String {
            val operationID = ChatSession.randomID()
            linkTriggers[operationID] = handler
            return """<a class="href-link" data-id="$operationID">"""
        }

        override fun textInput(handler: Consumer<String>): String {
            val operationID = ChatSession.randomID()
            txtTriggers[operationID] = handler
            //language=HTML
            return """<form class="reply-form">
                                       <textarea class="reply-input" data-id="$operationID" rows="3" placeholder="Type a message"></textarea>
                                       <button class="text-submit-button" data-id="$operationID">Send</button>
                                   </form>""".trimIndent()
        }

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