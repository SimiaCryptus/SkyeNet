package com.simiacryptus.skyenet.webui.session

import com.google.common.util.concurrent.MoreExecutors
import com.simiacryptus.skyenet.core.platform.*
import com.simiacryptus.skyenet.webui.chat.ChatServer
import com.simiacryptus.skyenet.webui.chat.ChatSocket
import com.simiacryptus.skyenet.webui.util.MarkdownUtil
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

abstract class SocketManagerBase(
    protected val session: Session,
    protected val dataStorage: DataStorage?,
    protected val user: User? = null,
    private val messageStates: LinkedHashMap<String, String> = dataStorage?.getMessages(
        user, session
    ) ?: LinkedHashMap(),
    private val applicationClass: Class<*>,
) : SocketManager {
    private val sockets: MutableSet<ChatSocket> = mutableSetOf()
    private val messageVersions = HashMap<String, AtomicInteger>()
    protected open val pool = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())

    override fun removeSocket(socket: ChatSocket) {
        sockets.remove(socket)
    }

    override fun addSocket(socket: ChatSocket) {
        sockets.add(socket)
    }

    private fun publish(
        out: String,
    ) {
        val socketsSnapshot = sockets.toTypedArray()
        socketsSnapshot.forEach {
            try {
                it.remote.sendString(out)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun newMessage(
        operationID: String = randomID(),
        spinner: String = SessionMessage.spinner,
        cancelable: Boolean = false
    ): SessionMessage {
        var responseContents = divInitializer(operationID, cancelable)
        send(responseContents)
        return SessionMessageImpl(responseContents, spinner)
    }

    inner class SessionMessageImpl(
        responseContents: String,
        spinner: String = SessionMessage.spinner
    ) : SessionMessage(responseContents, spinner) {
        override fun send(html: String) = this@SocketManagerBase.send(html)
    }

    fun send(out: String) {
        try {
            log.debug("Send Msg: $session - $out")
            val split = out.split(',', ignoreCase = false, limit = 2)
            val newVersion = setMessage(split[0], split[1])
            publish("${split[0]},$newVersion,${split[1]}")
        } catch (e: Exception) {
            log.debug("$session - $out", e)
        }
    }

    final override fun getReplay(): List<String> {
        return messageStates.entries.map {
            "${it.key},${messageVersions.computeIfAbsent(it.key) { AtomicInteger(1) }.get()},${it.value}"
        }
    }

    private fun setMessage(key: String, value: String): Int {
        if (messageStates.containsKey(key) && messageStates[key] == value) return -1
        dataStorage?.updateMessage(user, session, key, value)
        messageStates.put(key, value)
        return messageVersions.computeIfAbsent(key) { AtomicInteger(0) }.incrementAndGet()
    }

    final override fun onWebSocketText(socket: ChatSocket, message: String) {
        if (canWrite(user)) pool.submit {
            log.debug("$session - Received message: $message")
            try {
                val opCmdPattern = """![a-z]{3,7},.*""".toRegex()
                if (opCmdPattern.matches(message)) {
                    val id = message.substring(1, message.indexOf(","))
                    val code = message.substring(id.length + 2)
                    onCmd(id, code, socket)
                } else {
                    onRun(message, socket)
                }
            } catch (e: Exception) {
                log.warn("$session - Error processing message: $message", e)
                send("""${randomID()},<div class="error">${MarkdownUtil.renderMarkdown(e.message ?: "")}</div>""")
            }
        } else {
            log.warn("$session - Unauthorized message: $message")
            send("""${randomID()},<div class="error">Unauthorized message</div>""")
        }
    }

    open fun canWrite(user: User?) = ApplicationServices.authorizationManager.isAuthorized(
        applicationClass = applicationClass,
        user = user,
        operationType = AuthorizationManager.OperationType.Write
    )

    protected open fun onCmd(
        id: String,
        code: String,
        socket: ChatSocket
    ) {
    }

    protected abstract fun onRun(
        userMessage: String,
        socket: ChatSocket,
    )

    companion object {
        private val log = LoggerFactory.getLogger(ChatServer::class.java)

        fun randomID() = (0..5).map { ('a'..'z').random() }.joinToString("")
        fun divInitializer(operationID: String = randomID(), cancelable: Boolean): String =
            if (!cancelable) """$operationID,""" else
                """$operationID,<button class="cancel-button" data-id="$operationID">&times;</button>"""

    }

}