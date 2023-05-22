package com.simiacryptus.skyenet.body

import java.util.LinkedHashMap

abstract class PersistentSessionBase(
    sessionId: String,
    private val sessionDataStorage: SessionDataStorage,
    private val messageStates: LinkedHashMap<String, String> = sessionDataStorage.loadMessages(sessionId),
) : SessionBase(sessionId) {


    override fun send(out: String) {
        try {
            WebSocketServer.logger.debug("$sessionId - $out")
            val split = out.split(',', ignoreCase = false, limit = 2)
            setMessage(split[0], split[1])
            publish(out)
        } catch (e: Exception) {
            WebSocketServer.logger.debug("$sessionId - $out", e)
        }
    }

    protected open fun setMessage(key: String, value: String) {
        sessionDataStorage.updateMessage(sessionId, key, value)
        messageStates.put(key, value)
    }

    override fun getReplay(): List<String> {
        return messageStates.entries.map { "${it.key},${it.value}" }
    }


    override fun onWebSocketText(socket: WebSocketServer.MessageWebSocket, describedInstruction: String) {
        SkyenetCodingSessionServer.logger.debug("$sessionId - Received message: $describedInstruction")
        try {
            val opCmdPattern = """![a-z]{3,7},.*""".toRegex()
            if (opCmdPattern.matches(describedInstruction)) {
                val id = describedInstruction.substring(1, describedInstruction.indexOf(","))
                val code = describedInstruction.substring(id.length + 2)
                onCmd(id, code)
            } else {
                onRun(describedInstruction)
            }
        } catch (e: Exception) {
            SkyenetCodingSessionServer.logger.warn("$sessionId - Error processing message: $describedInstruction", e)
        }
    }

    protected open fun onRun(describedInstruction: String) {
        Thread {
            try {
                run(describedInstruction)
            } catch (e: Exception) {
                SkyenetCodingSessionServer.logger.warn(
                    "$sessionId - Error processing message: $describedInstruction",
                    e
                )
            }
        }.start()
    }

    protected open fun onCmd(id: String, code: String) {
    }

    protected abstract fun run(
        describedInstruction: String,
    )


}