package com.simiacryptus.skyenet.webui

import com.simiacryptus.openai.ChatMessage
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.Brain
import com.simiacryptus.skyenet.Heart
import com.simiacryptus.skyenet.heart.GroovyInterpreter
import com.simiacryptus.util.YamlDescriber
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.io.FileUtils
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.webapp.WebAppContext
import java.io.File
import java.io.InputStream


abstract class SkyenetSimpleSessionServer(
    val applicationName: String = "SkyenetSimpleSessionServer",
    val yamlDescriber: YamlDescriber = YamlDescriber(),
    val oauthConfig: String? = null,
    val autoRun: Boolean = false,
    resourceBase: String = "simpleSession",
    private val maxRetries: Int = 5,
    private var maxHistoryCharacters: Int = 4000,
) : WebSocketServer(resourceBase) {

    override fun configure(context: WebAppContext) {
        super.configure(context)

        if (null != oauthConfig) object : AuthenticatedWebsite() {
            override val redirectUri = "http://localhost:8080/oauth2callback"
            override val applicationName: String = this@SkyenetSimpleSessionServer.applicationName
            override fun getKey(): InputStream? {
                return FileUtils.openInputStream(File(oauthConfig))
            }
        }.configure(context)

        context.addServlet(
            ServletHolder(
                "yamlDescriptor",
                object : HttpServlet() {
                    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
                        resp.contentType = "text/plain"
                        resp.status = HttpServletResponse.SC_OK
                        val apiDescription = Brain.apiDescription(apiObjects(), yamlDescriber)
                        resp.writer.write(apiDescription)
                    }
                }),
            "/yamlDescriptor"
        )
    }

    protected open val apiKey: String = File(File(System.getProperty("user.home")), "openai.key").readText().trim()

    open val model: String? = null
    var useHistory = true

    abstract fun apiObjects(): Map<String, Any>

    open fun apiObjects(printBuffer: StringBuilder) = apiObjects() + mapOf(
        "sys" to MessageCallbacks(printBuffer),
    )

    open fun heart(apiObjects: Map<String, Any>): Heart = GroovyInterpreter(apiObjects)

    open class MessageCallbacks(private val printBuffer: StringBuilder) {
        open fun print(message: String) {
            printBuffer.append(message + "\n")
        }
    }

    val api = OpenAIClient(apiKey)
    override fun newSession(sessionId: String): SessionState {
        return object : SessionStateByID(sessionId) {
            override fun onWebSocketText(socket: MessageWebSocket, describedInstruction: String) {
                logger.debug("$sessionId - Received message: $describedInstruction")
                try {
                    val opCmdPattern = """![a-z]{3,7},.*""".toRegex()
                    if (opCmdPattern.matches(describedInstruction)) {
                        val id = describedInstruction.substring(1, describedInstruction.indexOf(","))
                        val code = describedInstruction.substring(id.length + 2)
                        operationStatus[id]?.onMessage(code)
                    } else {
                        val printBuffer = StringBuilder()
                        val apiObjects = apiObjects(printBuffer)
                        val heart = heart(apiObjects)
                        val brain: Brain = brain(apiObjects, heart.getLanguage())
                        Thread {
                            try {
                                run(describedInstruction, brain, heart, printBuffer)
                            } catch (e: Exception) {
                                logger.warn("$sessionId - Error processing message: $describedInstruction", e)
                            }
                        }.start()
                    }
                } catch (e: Exception) {
                    logger.warn("$sessionId - Error processing message: $describedInstruction", e)
                }
            }


        }
    }

    inner class OperationStatus(
        val created: Long = System.currentTimeMillis(),
        var operationID: String = "",
        var instruction: String = "",
        val language: String = "",
        var responseText: String = "",
        var responseCode: String = "",
        var resultValue: String = "",
        var resultOutput: String = "",
        var status: OperationState = OperationState.Pending,
        val thread: Thread = Thread.currentThread(),
    ) {
        fun onMessage(code: String) {
            if (code.lowercase() == "run") {
                runSemaphore.release()
                logger.info("$operationID - Running")
            } else if (code.lowercase() == "stop") {
                cancelFlag.set(true)
                thread.interrupt()
                logger.info("$operationID - Stopping")
            } else {
                logger.info("$operationID - Unknown command: $code")
            }
        }

        val runSemaphore = java.util.concurrent.Semaphore(0)
        val cancelFlag = java.util.concurrent.atomic.AtomicBoolean(false)
        val chatMessages
            get() = if (status == OperationState.Error) {
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
    }

    fun truncate(text: String, length: Int = 500): String {
        return if (text.length > length) text.substring(0, length - 3) + "..." else text
    }

    enum class OperationState {
        Pending, Implemented, Running, Complete, Error
    }

    private val operationStatus = mutableMapOf<String, OperationStatus>()

    open fun brain(
        apiObjects: Map<String, Any>,
        language: String,
        yamlDescriber: YamlDescriber = this@SkyenetSimpleSessionServer.yamlDescriber,
    ): Brain {
        val brain = object : Brain(
            api = api,
            apiObjects = apiObjects,
            language = language,
            yamlDescriber = yamlDescriber
        ) {
            override fun getChatMessages(apiDescription: String) =
                super.getChatMessages(apiDescription) + getPriorMessages(this)
        }
        if (null != model) brain.model = model!!
        return brain
    }

    protected open fun getPriorMessages(brain: Brain): List<ChatMessage> {
        val messages: java.util.ArrayList<ChatMessage> = ArrayList()
        if (useHistory) {
            val operationStatusList = operationStatus.values
                .filter { it.status != OperationState.Pending }
                .filter { it.status != OperationState.Running }
                .filter { it.status != OperationState.Implemented }
                .filter { !it.instruction.startsWith("!!!") }
                .sortedBy { it.created }
                .takeLast(maxRetries)
            for (operationStatus in operationStatusList) {
                messages.addAll(operationStatus.chatMessages)
            }
        }
        return truncate(messages, maxHistoryCharacters)
    }

    private fun truncate(messages: List<ChatMessage>, maxCharacters: Int): List<ChatMessage> {
        if (messages.isEmpty()) return messages
        var totalCharacters = 0
        val truncatedMessages = mutableListOf<ChatMessage>()
        for (message in messages.reversed()) {
            totalCharacters += message.content?.length ?: 0
            if (totalCharacters > maxCharacters) break
            truncatedMessages.add(message)
        }
        return truncatedMessages.reversed()
    }


    protected val spinner =
        """<div class="spinner-border" role="status"><span class="sr-only">Loading...</span></div>"""



    open fun SessionStateByID.run(
        describedInstruction: String,
        brain: Brain,
        heart: Heart,
        printBuffer: StringBuilder,
    ) {
        logger.info("$sessionId / ${System.identityHashCode(printBuffer)} - Processing message: $describedInstruction")
        val operationID = (0..5).map { ('a'..'z').random() }.joinToString("")
        val status = OperationStatus(
            operationID = operationID,
            instruction = describedInstruction,
            thread = Thread.currentThread(),
        )
        operationStatus[operationID] = status
        var retries = maxRetries
        var renderedResponse = ""
        var codedInstruction = ""
        var messageTrail = """$operationID,
            |<button class="cancel-button" data-id="$operationID">&times;</button>
        """.trimMargin()
        if (describedInstruction.startsWith("!!!")) {
            codedInstruction = describedInstruction.substringAfter("!!!")
            retries = 0
            //language=HTML
            messageTrail += """
                    |<div>
                    |<h3>Code:</h3>
                    |<pre><code class="language-${heart.getLanguage()}">
                    |$codedInstruction
                    |</code></pre>
                    |</div>
                    |""".trimMargin().trim()
        } else {
            //language=HTML
            messageTrail += """
                    |<div>
                    |<h3>Command:</h3>
                    |<pre>
                    |$describedInstruction
                    |</pre>
                    |</div>
                    |""".trimMargin().trim()
            //language=HTML
            send(
                """$messageTrail
                    |<div>
                    |<h3>Code:</h3>
                    |$spinner
                    |</div>
                    |""".trimMargin().trim()
            )
            var respondWithCode = brain.respondWithCode(describedInstruction)
            renderedResponse = getRenderedResponse(respondWithCode)
            codedInstruction = getGroovyCode(respondWithCode)
            status.responseText = renderedResponse
            status.responseCode = codedInstruction
            status.status = OperationState.Implemented

            //language=HTML
            messageTrail += """
                            |<div>
                            |<h3>Code:</h3>
                            |${renderedResponse}
                            |</div>
                        """.trimMargin().trim()
            //language=HTML
        }

        try {
            while (retries >= 0 && !status.cancelFlag.get()) {
                try {
                    send(
                        """
                            |$messageTrail
                            |<div>
                            |<button class="play-button" data-id="$operationID">▶</button>
                            |</div>
                        """.trimMargin().trim()
                    )
                    if(status.cancelFlag.get()) {
                        status.status = OperationState.Complete
                        break
                    }
                    if (!autoRun) status.runSemaphore.acquire()
                    if(status.cancelFlag.get()) {
                        status.status = OperationState.Complete
                        break
                    }
                    send(
                        """
                            |$messageTrail
                            |<div>
                            |$spinner
                            |</div>
                        """.trimMargin().trim()
                    )
                    status.status = OperationState.Running
                    val result = heart.run(codedInstruction)
                    status.resultValue = result.toString()
                    status.resultOutput = printBuffer.toString()
                    status.status = OperationState.Complete
                    //language=HTML
                    messageTrail += """
                                |<div>
                                |<h3>Output:</h3>
                                |<pre>
                                |$printBuffer
                                |</pre>
                                |
                                |<h3>Returns:</h3>
                                |<pre>
                                |${result}
                                |</pre>
                                |</div>
                                """.trimMargin().trim()
                    printBuffer.clear()
                    send(messageTrail)
                    break
                } catch (e: Exception) {
                    //language=HTML
                    messageTrail += """
                                |<div>
                                |<h3>Error:</h3>
                                |<pre>
                                |${toString(e)}
                                |</pre>
                                |</div>
                                """.trimMargin().trim()
                    status.status = OperationState.Error
                    status.resultOutput = printBuffer.toString()
                    status.resultValue = toString(e)
                    if (retries <= 0 || status.cancelFlag.get()) {
                        send(
                            //language=HTML
                            """$messageTrail
                        |<div>
                        |<h3>Out of Retries!</h3>
                        |</div>
                        |""".trimMargin().trim()
                        )
                        break
                    } else {
                        retries--
                        //language=HTML
                        send(
                            """
                            |$messageTrail
                            |<div>
                            |<h3>New Code:</h3>
                            |$spinner
                            |</div>
                            |""".trimMargin().trim()
                        )
                        val respondWithCode = brain.fixCommand2(describedInstruction, codedInstruction, e)
                        renderedResponse = getRenderedResponse(respondWithCode)
                        codedInstruction = getGroovyCode(respondWithCode)
                        status.responseText = renderedResponse
                        status.responseCode = codedInstruction
                        status.status = OperationState.Implemented
                        //language=HTML
                        messageTrail += """
                            |<div>
                            |<h3>New Code:</h3>
                            |$renderedResponse
                            |</div>
                        """.trimMargin().trim()
                    }
                }
            }
        } catch (e: Exception) {
            //language=HTML
            messageTrail += """
                            |<div>
                            |<h3>Error:</h3>
                            |<pre>
                            |${e.message}
                            |</pre>
                            |</div>
                        """.trimMargin().trim()
            send(messageTrail)
        }

    }

    open fun toString(e: Throwable): String {
        val sw = java.io.StringWriter()
        e.printStackTrace(java.io.PrintWriter(sw))
        return sw.toString()
    }

    companion object {
        val logger = org.slf4j.LoggerFactory.getLogger(SkyenetSimpleSessionServer::class.java)
        fun getRenderedResponse(respondWithCode: Pair<String, List<Pair<String, String>>>) =
            respondWithCode.second.joinToString("\n") {
                var language = it.first
                if (language == "code") language = "groovy"
                if (language == "text") {
                    //language=HTML
                    """
                    |<div>
                    |${it.second}
                    |</div>
                    |""".trimMargin().trim()
                } else {
                    //language=HTML
                    """
                    |<pre><code class="language-$language">
                    |${it.second}
                    |</code></pre>
                    |""".trimMargin().trim()
                }
            }

        fun getGroovyCode(respondWithCode: Pair<String, List<Pair<String, String>>>) =
            respondWithCode.second.joinToString("\n") {
                var language = it.first
                if (language == "code") language = "groovy"
                if (language == "groovy") {
                    """
                    |${it.second}
                    |""".trimMargin().trim()
                } else {
                    ""
                }
            }

    }


}