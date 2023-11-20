package com.simiacryptus.skyenet.session

import com.google.common.util.concurrent.MoreExecutors
import com.simiacryptus.skyenet.ApplicationBase
import com.simiacryptus.skyenet.chat.ChatServer
import com.simiacryptus.skyenet.chat.ChatSocket
import com.simiacryptus.skyenet.platform.ApplicationServices
import com.simiacryptus.skyenet.platform.AuthorizationManager
import com.simiacryptus.skyenet.platform.DataStorage
import com.simiacryptus.skyenet.util.MarkdownUtil
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

abstract class SessionBase(
    protected val sessionId: String,
    private val dataStorage: DataStorage?,
    protected val userId: String? = null,
    private val messageStates: LinkedHashMap<String, String> = dataStorage?.getMessages(
        userId, sessionId
    ) ?: LinkedHashMap(),
    private val applicationClass: Class<out ApplicationBase>,
) : SessionInterface {
    private val sockets: MutableSet<ChatSocket> = mutableSetOf()
    private val messageVersions = HashMap<String, AtomicInteger>()
    protected open val pool = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())

    override fun removeSocket(socket: ChatSocket) {
        sockets.remove(socket)
    }

    override fun addSocket(socket: ChatSocket) {
        sockets.add(socket)
    }

    protected fun publish(
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

    fun newSessionDiv(
        operationID: String, spinner: String, cancelable: Boolean = false
    ): SessionDiv {
        var responseContents = divInitializer(operationID, cancelable)
        send(responseContents)
        return object : SessionDiv() {
            override fun append(htmlToAppend: String, showSpinner: Boolean) {
                if (htmlToAppend.isNotBlank()) {
                    responseContents += """<div>$htmlToAppend</div>"""
                }
                val spinner1 = if (showSpinner) """<div>$spinner</div>""" else ""
                return this@SessionBase.send("""$responseContents$spinner1""")
            }

            override fun sessionID(): String {
                return this@SessionBase.sessionId
            }

            override fun divID(): String {
                return operationID
            }
        }
    }

    fun send(out: String) {
        try {
            log.debug("Send Msg: $sessionId - $out")
            val split = out.split(',', ignoreCase = false, limit = 2)
            val newVersion = setMessage(split[0], split[1])
            publish("${split[0]},$newVersion,${split[1]}")
        } catch (e: Exception) {
            log.debug("$sessionId - $out", e)
        }
    }

    final override fun getReplay(): List<String> {
        return messageStates.entries.map {
            "${it.key},${messageVersions.computeIfAbsent(it.key) { AtomicInteger(1) }.get()},${it.value}"
        }
    }

    private fun setMessage(key: String, value: String): Int {
        if (messageStates.containsKey(key) && messageStates[key] == value) return -1
        dataStorage?.updateMessage(userId, sessionId, key, value)
        messageStates.put(key, value)
        return messageVersions.computeIfAbsent(key) { AtomicInteger(0) }.incrementAndGet()
    }

    final override fun onWebSocketText(socket: ChatSocket, message: String) {
        if (canWrite(socket.user?.email)) pool.submit {
            log.debug("$sessionId - Received message: $message")
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
                log.warn("$sessionId - Error processing message: $message", e)
                send("""${randomID()},<div class="error">${MarkdownUtil.renderMarkdown(e.message ?: "")}</div>""")
            }
        } else {
            log.warn("$sessionId - Unauthorized message: $message")
            send("""${randomID()},<div class="error">Unauthorized message</div>""")
        }
    }

    open fun canWrite(user: String?) = ApplicationServices.authorizationManager.isAuthorized(
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
        private val log = org.slf4j.LoggerFactory.getLogger(ChatServer::class.java)

        fun randomID() = (0..5).map { ('a'..'z').random() }.joinToString("")
        fun divInitializer(operationID: String = randomID(), cancelable: Boolean): String =
            if (!cancelable) """$operationID,""" else
                """$operationID,<button class="cancel-button" data-id="$operationID">&times;</button>"""

    }

}