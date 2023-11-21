package com.simiacryptus.skyenet

import com.simiacryptus.skyenet.chat.ChatSocket
import com.simiacryptus.skyenet.platform.DataStorage
import com.simiacryptus.skyenet.platform.SessionID
import com.simiacryptus.skyenet.platform.UserInfo
import com.simiacryptus.skyenet.session.SessionBase
import com.simiacryptus.skyenet.session.SessionDiv
import java.util.function.Consumer

abstract class ApplicationSession(
    sessionId: SessionID,
    userId: UserInfo?,
    dataStorage: DataStorage?,
    applicationClass: Class<*>,
) : SessionBase(
    sessionId = sessionId,
    dataStorage = dataStorage,
    userId = userId,
    applicationClass = applicationClass,
) {
    private val threads = mutableMapOf<String, Thread>()
    private val linkTriggers = mutableMapOf<String, Consumer<Unit>>()
    private val txtTriggers = mutableMapOf<String, Consumer<String>>()

    override fun onRun(userMessage: String, socket: ChatSocket) {
        val operationID = randomID()
        val sessionDiv = newSessionDiv(operationID, spinner, true)
        threads[operationID] = Thread.currentThread()
        processMessage(sessionId, userId = userId, userMessage, this, sessionDiv, socket)
    }

    override fun onCmd(id: String, code: String, socket: ChatSocket) {
        if (code == "cancel") {
            threads[id]?.interrupt()
        } else if (code == "link") {
            val consumer = linkTriggers[id]
            consumer ?: throw IllegalArgumentException("No link handler found")
            consumer.accept(Unit)
        } else {
            throw IllegalArgumentException("Unknown command: $code")
        }
    }

    val spinner: String get() = """<div>${ApplicationBase.spinner}</div>"""
//        val playButton: String get() = """<button class="play-button" data-id="$operationID">▶</button>"""
//        val cancelButton: String get() = """<button class="cancel-button" data-id="$operationID">&times;</button>"""
//        val regenButton: String get() = """<button class="regen-button" data-id="$operationID">♲</button>"""

    fun hrefLink(linkText: String, classname: String = """href-link""", handler: Consumer<Unit>): String {
        val operationID = randomID()
        linkTriggers[operationID] = handler
        return """<a class="$classname" data-id="$operationID">$linkText</a>"""
    }
    fun textInput(handler: Consumer<String>): String {
        val operationID = randomID()
        txtTriggers[operationID] = handler
        //language=HTML
        return """<form class="reply-form">
                   <textarea class="reply-input" data-id="$operationID" rows="3" placeholder="Type a message"></textarea>
                   <button class="text-submit-button" data-id="$operationID">Send</button>
               </form>""".trimIndent()
    }


    abstract fun processMessage(
        sessionId: SessionID,
        userId: UserInfo?,
        userMessage: String,
        session: ApplicationSession,
        sessionDiv: SessionDiv,
        socket: ChatSocket
    )
}