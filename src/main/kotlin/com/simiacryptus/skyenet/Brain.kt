package com.simiacryptus.skyenet

import com.simiacryptus.openai.proxy.ChatProxy
import com.simiacryptus.openai.proxy.Description
import java.io.File

/**
 * The brain is the interface to the OpenAI API for the SkyeNet system
 */
interface Brain {

    data class AssistantCommand(
        val command: String = "",
        val methods: List<String> = listOf(),
        val types: Map<String, String> = mapOf(),
    )

    data class AssistantInstruction(
        val javascript: String = "",
    )

    fun interpretCommand(command: AssistantCommand): AssistantInstruction


    data class DictationBuffer(
        val text: String = ""
    )

    data class DictationBufferResult(
        val commandRecognized: Boolean = false,
        @Description("Full text of the command that was recognized, with dictation errors corrected")
        val command: String = "",
    )

    fun listenForCommand(inputBuffer: DictationBuffer): DictationBufferResult

}