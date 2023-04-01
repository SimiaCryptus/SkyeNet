package com.simiacryptus.skyenet

import com.simiacryptus.openai.OpenAIClient
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
class Ears(
    val brain: Brain,
    val keyFile: File = File("C:\\Users\\andre\\code\\all-projects\\openai.key"),
    val secondsPerAudioPacket : Double = 0.25,
) {

    fun timeout(ms: Long): () -> Boolean {
        val endTime = System.currentTimeMillis() + ms
        return { System.currentTimeMillis() < endTime }
    }

    fun listenForCommand(
        minCaptureMs: Int = 1000,
        continueFn: () -> Boolean = timeout(120, TimeUnit.SECONDS),
        rawBuffer: Deque<ByteArray> = startAudioCapture(continueFn),
        commandHandler: (command: String) -> Unit,
    ) {
        val buffer = StringBuilder()
        val commandsProcessed = AtomicInteger(0)
        var lastCommandCheckTime = System.currentTimeMillis()
        startDictationListener(
            continueFn = { continueFn() && 0 == commandsProcessed.get() },
            rawBuffer = rawBuffer
        ) {
            buffer.append(it)
            if (System.currentTimeMillis() - lastCommandCheckTime > minCaptureMs) {
                log.info("Checking for command: $buffer")
                lastCommandCheckTime = System.currentTimeMillis()
                val inputBuffer = Brain.DictationBuffer(buffer.toString())
                brain.listenForCommand(inputBuffer).let { result ->
                    if (result.commandRecognized) {
                        log.info("Command recognized: ${result.command}")
                        commandsProcessed.incrementAndGet()
                        buffer.clear()
                        commandHandler(result.command)
                    }
                }
            }
        }
    }

    fun startDictationListener(
        client: OpenAIClient = OpenAIClient(FileUtils.readFileToString(keyFile, "UTF-8").trim()),
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

    fun startAudioCapture(continueFn: () -> Boolean): ConcurrentLinkedDeque<ByteArray> {
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