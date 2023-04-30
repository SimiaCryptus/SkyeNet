package com.simiacryptus.skyenet

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.proxy.ChatProxy
import com.simiacryptus.util.*
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * The ears are the interface to the audio input for the SkyeNet system
 */
@Suppress("MemberVisibilityCanBePrivate")
open class Ears(
    val api: OpenAIClient,
    val secondsPerAudioPacket : Double = 0.25,
) {

    interface CommandRecognizer {
        fun listenForCommand(inputBuffer: DictationBuffer): CommandRecognized

        data class DictationBuffer(
            val text: String? = null,
        )

        data class CommandRecognized(
            val commandRecognized: Boolean? = null,
            val command: String? = null,
        )
    }

    open val commandRecognizer = ChatProxy(
        clazz = CommandRecognizer::class.java,
        api = api
    ).create()

    open fun timeout(ms: Long): () -> Boolean {
        val endTime = System.currentTimeMillis() + ms
        return { System.currentTimeMillis() < endTime }
    }

    open fun listenForCommand(
        client: OpenAIClient,
        minCaptureMs: Int = 1000,
        continueFn: () -> Boolean = timeout(120, TimeUnit.SECONDS),
        rawBuffer: Deque<ByteArray> = startAudioCapture(continueFn),
        commandHandler: (command: String) -> Unit,
    ) {
        val buffer = StringBuilder()
        val commandsProcessed = AtomicInteger(0)
        var lastCommandCheckTime = System.currentTimeMillis()
        startDictationListener(
            client,
            continueFn = { continueFn() && 0 == commandsProcessed.get() },
            rawBuffer = rawBuffer
        ) {
            buffer.append(it)
            if (System.currentTimeMillis() - lastCommandCheckTime > minCaptureMs) {
                log.info("Checking for command: $buffer")
                lastCommandCheckTime = System.currentTimeMillis()
                val inputBuffer = CommandRecognizer.DictationBuffer(buffer.toString())
                commandRecognizer.listenForCommand(inputBuffer).let { result ->
                    if (result.commandRecognized == true) {
                        log.info("Command recognized: ${result.command}")
                        commandsProcessed.incrementAndGet()
                        buffer.clear()
                        if(null != result.command) commandHandler(result.command)
                    }
                }
            }
        }
    }

    open fun startDictationListener(
        client: OpenAIClient,
        continueFn: () -> Boolean = timeout(60, TimeUnit.SECONDS),
        rawBuffer: Deque<ByteArray> = startAudioCapture(continueFn),
        textAppend: (String) -> Unit,
    ) {
        val wavBuffer = ConcurrentLinkedDeque<ByteArray>()
        Thread({
            try {
                LookbackLoudnessWindowBuffer(rawBuffer, wavBuffer, continueFn).run()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }, "dictation-audio-processor").start()
        val dictationProcessor = TranscriptionProcessor(client, wavBuffer, continueFn) {
            log.info("Dictation: $it")
            textAppend(it)
        }
        val dictationThread = Thread({
            try {
                dictationProcessor.run()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }, "dictation-api-processor")
        dictationThread.start()
        dictationThread.join()
    }

    open fun startAudioCapture(continueFn: () -> Boolean): ConcurrentLinkedDeque<ByteArray> {
        val rawBuffer = ConcurrentLinkedDeque<ByteArray>()
        Thread({
            try {
                AudioRecorder(rawBuffer, secondsPerAudioPacket, continueFn).run()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }, "dication-audio-recorder").start()
        return rawBuffer
    }

    private fun timeout(count: Long, timeUnit: TimeUnit): () -> Boolean = timeout(timeUnit.toMillis(count))

    companion object {
        val log = LoggerFactory.getLogger(Ears::class.java)
    }

}