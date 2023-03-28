package com.simiacryptus.skyenet

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.util.AudioRecorder
import com.simiacryptus.util.LoudnessWindowBuffer
import org.apache.commons.io.FileUtils
import java.io.File
import java.lang.StringBuilder
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.TimeUnit

@Suppress("MemberVisibilityCanBePrivate")
object Ears {
    private val keyFile: File = File("C:\\Users\\andre\\code\\all-projects\\openai.key")

    fun timeout(ms: Long): () -> Boolean {
        val endTime = System.currentTimeMillis() + ms
        val continueFn: () -> Boolean = { System.currentTimeMillis() < endTime }
        return continueFn
    }

    fun startExecutionListener(count: Long = 120, timeUnit: TimeUnit = TimeUnit.SECONDS) {
        startExecutionTestListener(timeout(timeUnit, count))
    }

    fun startExecutionTestListener(continueFn: () -> Boolean) {
        val commandHandler = Hands::command
        val buffer = StringBuilder()
        var lastCommandCheckTime = System.currentTimeMillis()
        startDictationListener(
            continueFn = continueFn,
            textAppend = {
                buffer.append(it)
                if(System.currentTimeMillis() - lastCommandCheckTime > 100) {
                    lastCommandCheckTime = System.currentTimeMillis()
                    Brain.brain.create().listenForCommand(Brain.DictationBuffer(buffer.toString())).let { result ->
                        if (result.commandRecognized) {
                            commandHandler(result.command)
                            buffer.clear()
                        }
                    }
                }
            })
    }

    fun startDictationListener(
        continueFn: () -> Boolean = timeout(TimeUnit.SECONDS, 60),
        textAppend: (String) -> Unit = System.out::println,
    ) {
        if (!keyFile.exists()) return
        val client: OpenAIClient = OpenAIClient(FileUtils.readFileToString(keyFile, "UTF-8").trim())
        val rawBuffer = ConcurrentLinkedDeque<ByteArray>()
        Thread({
            try {
                AudioRecorder(rawBuffer, 0.05, continueFn).run()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }, "dication-audio-recorder").start()
        val wavBuffer = ConcurrentLinkedDeque<ByteArray>()
        Thread({
            try {
                LoudnessWindowBuffer(rawBuffer, wavBuffer, continueFn).run()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }, "dictation-audio-processor").start()
        val dictationPump = DicPump(client, wavBuffer, continueFn, textAppend)
        val dictationThread = Thread({
            try {
                dictationPump.run()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }, "dictation-api-processor")
        dictationThread.start()
        dictationThread.join()
    }

    private fun timeout(timeUnit: TimeUnit, count: Long): () -> Boolean = timeout(timeUnit.toMillis(count))

    private class DicPump(
        val client: OpenAIClient,
        private val audioBuffer: Deque<ByteArray>,
        val continueFn: () -> Boolean,
        val buffer: (String) -> Unit = {},
        var prompt: String = "",
    ) {
        fun run() {
            while (this.continueFn() || audioBuffer.isNotEmpty()) {
                val recordAudio = audioBuffer.poll()
                if (null == recordAudio) {
                    Thread.sleep(1)
                } else {
                    var text = client.dictate(recordAudio, prompt)
                    if (prompt.isNotEmpty()) text = " $text"
                    val newPrompt = (prompt + text).split(" ").takeLast(32).joinToString(" ")
                    prompt = newPrompt
                    println(text)
                    buffer(text)
                }
            }
        }
    }
}

