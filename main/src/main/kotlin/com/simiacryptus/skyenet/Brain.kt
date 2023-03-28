package com.simiacryptus.skyenet

import com.simiacryptus.openai.proxy.ChatProxy
import java.io.File

interface Brain {

    data class AssistantCommand(
        val spokenCommand: String = "",
        val symbolsDefined: List<String> = listOf(),
    )

    data class DictationBuffer(
        val text: String = ""
    )

    fun listenForCommand(inputBuffer: DictationBuffer): DictationBufferResult

    data class DictationBufferResult(
        val commandRecognized: Boolean = false,
        val command: String = "",
        val outputBuffer: DictationBuffer = DictationBuffer(),
    )



    fun commandToCode(command: AssistantCommand): AssistantInstruction

    data class AssistantInstruction(
        val scala: String = "",
    )

    companion object {

        val brain =
            ChatProxy(Brain::class.java, File("C:\\Users\\andre\\code\\all-projects\\openai.key").readText().trim())

    }
}