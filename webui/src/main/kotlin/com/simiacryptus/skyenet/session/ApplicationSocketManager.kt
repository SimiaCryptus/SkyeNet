package com.simiacryptus.skyenet.session

import com.simiacryptus.openai.OpenAIAPI
import com.simiacryptus.skyenet.ApplicationBase
import com.simiacryptus.skyenet.chat.ChatSocket
import com.simiacryptus.skyenet.platform.DataStorage
import com.simiacryptus.skyenet.platform.Session
import com.simiacryptus.skyenet.platform.User
import java.util.function.Consumer

abstract class ApplicationSocketManager(
    session: Session,
    userId: User?,
    dataStorage: DataStorage?,
    applicationClass: Class<*>,
) : SocketManagerBase(
    session = session,
    dataStorage = dataStorage,
    userId = userId,
    applicationClass = applicationClass,
) {
    private val threads = mutableMapOf<String, Thread>()
    private val linkTriggers = mutableMapOf<String, Consumer<Unit>>()
    private val txtTriggers = mutableMapOf<String, Consumer<String>>()

    override fun onRun(userMessage: String, socket: ChatSocket) {
        val operationID = randomID()
        threads[operationID] = Thread.currentThread()
        newSession(session, user = userId, userMessage, this, socket.api)
    }

    val applicationInterface by lazy { ApplicationInterface(this) }

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

    abstract fun newSession(
        session: Session,
        user: User?,
        userMessage: String,
        socketManager: ApplicationSocketManager,
        api: OpenAIAPI
    )

    companion object {
        val spinner: String get() = """<div>${ApplicationBase.spinner}</div>"""
//        val playButton: String get() = """<button class="play-button" data-id="$operationID">▶</button>"""
//        val cancelButton: String get() = """<button class="cancel-button" data-id="$operationID">&times;</button>"""
//        val regenButton: String get() = """<button class="regen-button" data-id="$operationID">♲</button>"""
    }
}