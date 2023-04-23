//package com.simiacryptus.skyenet
//
//import com.simiacryptus.openai.OpenAIClient
//import com.simiacryptus.skyenet.heart.KotlinInterpreter
//import com.simiacryptus.util.AudioRecorder
//import com.simiacryptus.util.LookbackLoudnessWindowBuffer
//import com.simiacryptus.util.TranscriptionProcessor
//import org.junit.jupiter.api.Test
//import java.awt.Button
//import java.io.File
//import java.util.concurrent.ConcurrentLinkedDeque
//import java.util.concurrent.TimeUnit
//import javax.script.ScriptEngineManager
//import javax.swing.JFrame
//import javax.swing.JPanel
//
//class DevTests {
//
//    private val openAIKey = "C:\\Users\\andre\\code\\all-projects\\openai.key"
//    private val googleSpeechKey = "C:\\Users\\andre\\code\\aicoder\\SkyeNet\\google_speech_api.key.json"
//    private val apiKey = File(openAIKey).readText().trim()
//
//    /**
//     * This is the main test demonstrating the full functionality of the system
//     */
//    @Test
//    fun testHead() {
//        val apiObjects = mapOf(
//            "toolObj" to TestTools(googleSpeechKey)
//        )
//        val body = Body(
//            api = OpenAIClient(apiKey),
//            apiObjects = apiObjects
//        )
//        val head = Head(body = body, ears = Ears(api = OpenAIClient(apiKey)))
//        val jFrame = head.start(api = OpenAIClient(apiKey))
//        while (jFrame.isVisible) {
//            Thread.sleep(100)
//        }
//    }
//
//    @Test
//    fun testBody() {
//        val body = Body(
//            api = OpenAIClient(apiKey),
//            apiObjects = mapOf(
//                "toolObj" to TestTools(googleSpeechKey)
//            )
//        )
//        val head = Head(body = body, ears = Ears(api = OpenAIClient(apiKey)))
//        val jFrame = head.start(api = OpenAIClient(apiKey))
//        while (jFrame.isVisible) {
//            Thread.sleep(100)
//        }
//    }
//
//    @Test
//    fun testKotlinScript() {
//        val scriptEngineManager = ScriptEngineManager()
//        scriptEngineManager.engineFactories.forEach {
//            println(
//                """
//                EngineName: ${it.engineName}
//                EngineVersion: ${it.engineVersion}
//                LanguageName: ${it.languageName}
//                LanguageVersion: ${it.languageVersion}
//                Names: ${it.names}
//                Extensions: ${it.extensions}
//                MimeTypes: ${it.mimeTypes}
//            """.trimIndent()
//            )
//        }
//        val engineByExtension = scriptEngineManager.getEngineByExtension("kts")
//
//        require(engineByExtension != null) { "No kotlin script engine found" }
//    }
//
//    @Test
//    fun testAutoScript() {
//        KotlinInterpreter().run("println(\"Hello World\")")
//    }
//
//
//    @Test
//    fun testHeart() {
//        KotlinInterpreter(
//            defs = mapOf(
//                "toolObj" to TestTools(googleSpeechKey)
//            )
//        ).run("toolObj.speak(\"Hello World\")")
//    }
//
//    @Test
//    fun testSpeak() {
//        try {
//            KotlinInterpreter().run(
//                """
//            speak("Hello World");
//            """.trimIndent()
//            )
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//
//    @Test
//    fun testSpeak_Direct() {
//        TestTools(googleSpeechKey).speak(
//            """
//        Once upon a time, there was a magical land filled with talking animals and fairies. One day, a brave knight set out on a quest to save a princess from an evil dragon. Along the way, he met a group of friendly dwarves who helped him on his journey. When they finally reached the dragon's lair, the knight and his companions battled fiercely until they emerged victorious. The princess was saved and the kingdom rejoiced. From that day on, the knight and his new friends lived happily ever after.
//        """.trimIndent()
//        );
//    }
//
//    @Test
//    fun testFace() {
//        val face = Face()
//        val frame = JFrame("SkyeNet - A Helpful Pup")
//        frame.contentPane = face.panel1
//        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
//        frame.pack()
//        frame.isVisible = true
//        while (frame.isVisible) {
//            Thread.sleep(100)
//        }
//    }
//
//    @Test
//    fun testEars() {
//        val button = Button("Listen")
//        val ears = Ears(api = OpenAIClient(apiKey))
//        button.addActionListener {
//            Thread {
//                button.isEnabled = false
//                try {
//                    ears.listenForCommand(client = OpenAIClient(apiKey)) {
//                        val body = Body(api = OpenAIClient(apiKey), mapOf("toolObj" to TestTools(googleSpeechKey)))
//                        body.brain.implement(it).let {
//                            println(it)
//                            body.heart.run(it)
//                        }
//                    }
//                } finally {
//                    button.isEnabled = true
//                }
//            }.start()
//        }
//        val frame = JFrame("Test Assistant")
//        val panel = JPanel()
//        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
//        panel.add(button)
//        frame.add(panel)
//        frame.setSize(300, 100)
//        frame.isVisible = true
//        while (frame.isVisible) {
//            Thread.sleep(100)
//        }
//    }
//
//    @Test
//    fun testHands() {
//        val apiObjects = mapOf("toolObj" to TestTools(googleSpeechKey))
//        val body = Body(api = OpenAIClient(apiKey), apiObjects)
//        body.brain.implement(
//            "Find the largest prime number less than 1000 and show it in a dialog box."
//        ).let {
//            println(it)
//            body.heart.run(it)
//        }
//    }
//
//    @Test
//    fun testPOA() {
//        val body = Body(api = OpenAIClient(apiKey), mapOf("toolObj" to TestTools(googleSpeechKey)))
//        body.brain.implement(
//            "Speak the Pledge of Allegiance."
//        ).let {
//            println(it)
//            body.heart.run(it)
//        }
//    }
//
//    @Test
//    fun testDictate() {
//        if (!File(openAIKey).exists()) return
//        val client: OpenAIClient = OpenAIClient(apiKey)
//        val endTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(120)
//        val continueFn: () -> Boolean = { System.currentTimeMillis() < endTime }
//        val rawBuffer = ConcurrentLinkedDeque<ByteArray>()
//        Thread({
//            try {
//                AudioRecorder(rawBuffer, 0.25, continueFn).run()
//            } catch (e: Throwable) {
//                e.printStackTrace()
//            }
//        }, "dication-audio-recorder").start()
//        val wavBuffer = ConcurrentLinkedDeque<ByteArray>()
//        Thread({
//            try {
//                LookbackLoudnessWindowBuffer(rawBuffer, wavBuffer, continueFn).run()
//            } catch (e: Throwable) {
//                e.printStackTrace()
//            }
//        }, "dictation-audio-processor").start()
//        val dictationProcessor = TranscriptionProcessor(client, wavBuffer, continueFn) { println(it) }
//        val dictationThread = Thread({
//            try {
//                dictationProcessor.run()
//            } catch (e: Throwable) {
//                e.printStackTrace()
//            }
//        }, "dictation-api-processor")
//        dictationThread.start()
//        dictationThread.join()
//    }
//
//}