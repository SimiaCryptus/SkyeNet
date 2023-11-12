package com.simiacryptus.skyenet.sessions

import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

abstract class PersistentSessionBase(
    sessionId: String,
    private val sessionDataStorage: SessionDataStorage,
    private val messageStates: LinkedHashMap<String, String> = sessionDataStorage.loadMessages(sessionId),
) : SessionBase(sessionId) {

    private val messageVersions = HashMap<String, AtomicInteger>()

    companion object {
        val log = org.slf4j.LoggerFactory.getLogger(WebSocketServer::class.java)
    }

    public override fun send(out: String) {
        try {
            log.debug("$sessionId - $out")
            val split = out.split(',', ignoreCase = false, limit = 2)
            val newVersion = setMessage(split[0], split[1])
            publish("${split[0]},$newVersion,${split[1]}")
        } catch (e: Exception) {
            log.debug("$sessionId - $out", e)
        }
    }

    protected open fun setMessage(key: String, value: String): Int {
        if (messageStates.containsKey(key) && messageStates[key] == value) return -1
        sessionDataStorage.updateMessage(sessionId, key, value)
        messageStates.put(key, value)
        return messageVersions.computeIfAbsent(key) { AtomicInteger(0) }.incrementAndGet()
    }

    override fun getReplay(): List<String> {
        return messageStates.entries.map {
            "${it.key},${
                messageVersions.computeIfAbsent(it.key) { AtomicInteger(1) }.get()
            },${it.value}"
        }
    }


    open val pool = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())

    override fun onWebSocketText(socket: MessageWebSocket, message: String) {
        pool.submit {
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
            }
        }
    }

    protected open fun onRun(describedInstruction: String, socket: MessageWebSocket) {
        Thread {
            try {
                run(describedInstruction, socket)
            } catch (e: Exception) {
                log.warn(
                    "$sessionId - Error processing message: $describedInstruction",
                    e
                )
            }
        }.start()
    }

    protected open fun onCmd(id: String, code: String, socket: MessageWebSocket) {
    }

    protected abstract fun run(
        userMessage: String,
        socket: MessageWebSocket,
    )


}