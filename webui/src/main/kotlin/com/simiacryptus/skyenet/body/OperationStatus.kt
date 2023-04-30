package com.simiacryptus.skyenet.body

import com.fasterxml.jackson.annotation.JsonIgnore
import com.simiacryptus.openai.ChatMessage


class OperationStatus(
    val created: Long = System.currentTimeMillis(),
    val language: String = "",
    val operationID: String = "",
    val instruction: String = "",
    var status: OperationState = OperationState.Pending,
    var responseText: String = "",
    var responseCode: String = "",
    var resultValue: String = "",
    var resultOutput: String = "",
    @JsonIgnore val thread: Thread? = null,
) {
    enum class OperationState {
        Pending, Implemented, Running, Complete, Error
    }

    fun onMessage(code: String) {
        if (code.lowercase() == "run") {
            runSemaphore.release()
            SkyenetSessionServer.logger.debug("$operationID - Running")
        } else if (code.lowercase() == "stop") {
            cancelFlag.set(true)
            thread?.interrupt()
            SkyenetSessionServer.logger.debug("$operationID - Stopping")
        } else {
            SkyenetSessionServer.logger.warn("$operationID - Unknown command: $code")
        }
    }

    @JsonIgnore val runSemaphore = java.util.concurrent.Semaphore(0)
    @JsonIgnore val cancelFlag = java.util.concurrent.atomic.AtomicBoolean(false)
    val chatMessages
        @JsonIgnore get() = if (status == OperationState.Error) {
            listOf(
                ChatMessage(
                    ChatMessage.Role.user, """
                                    |${truncate(instruction)}
                                """.trimMargin()
                ), ChatMessage(
                    ChatMessage.Role.assistant, """
                                    |```${language}
                                    |${truncate(responseCode)}
                                    |```
                                    |""".trimMargin().trim()
                ),
                ChatMessage(
                    ChatMessage.Role.system, """
                                    |Error:
                                    |```
                                    |${truncate(resultValue)}
                                    |```
                                """.trimMargin().trim()
                )
            )
        } else {
            listOf(
                ChatMessage(
                    ChatMessage.Role.user, """
                                    |${truncate(instruction)}
                                """.trimMargin()
                ), ChatMessage(
                    ChatMessage.Role.assistant, """
                                    |```${language}
                                    |${truncate(responseCode)}
                                    |```
                                    |""".trimMargin().trim()
                ),
                ChatMessage(
                    ChatMessage.Role.system, """
                                    |Output:
                                    |```
                                    |${truncate(resultOutput)}
                                    |```
                                    |
                                    |Returns:
                                    |```
                                    |${truncate(resultValue)}
                                    |```
                                """.trimMargin().trim()
                )
            )
        }
    fun truncate(text: String, length: Int = 500): String {
        return if (text.length > length) text.substring(0, length - 3) + "..." else text
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OperationStatus

        if (created != other.created) return false
        if (language != other.language) return false
        if (operationID != other.operationID) return false
        if (instruction != other.instruction) return false
        if (status != other.status) return false
        if (responseText != other.responseText) return false
        if (responseCode != other.responseCode) return false
        if (resultValue != other.resultValue) return false
        return resultOutput == other.resultOutput
    }

    override fun hashCode(): Int {
        var result = created.hashCode()
        result = 31 * result + language.hashCode()
        result = 31 * result + operationID.hashCode()
        result = 31 * result + instruction.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + responseText.hashCode()
        result = 31 * result + responseCode.hashCode()
        result = 31 * result + resultValue.hashCode()
        result = 31 * result + resultOutput.hashCode()
        return result
    }


}