package com.simiacryptus.skyenet.body

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.Brain
import com.simiacryptus.skyenet.Brain.Companion.extractCodeBlocks
import com.simiacryptus.skyenet.Heart
import com.simiacryptus.skyenet.OutputInterceptor
import com.simiacryptus.skyenet.body.SessionServerUtil.getCode
import com.simiacryptus.skyenet.body.SessionServerUtil.getHistory
import com.simiacryptus.skyenet.body.SessionServerUtil.getRenderedResponse
import com.simiacryptus.util.YamlDescriber
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.io.FileUtils
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.webapp.WebAppContext
import java.io.File
import java.io.InputStream

abstract class SkyenetSessionServer(
    val applicationName: String,
    val yamlDescriber: YamlDescriber = YamlDescriber(),
    val oauthConfig: String? = null,
    val autoRun: Boolean = false,
    resourceBase: String = "simpleSession",
    private val maxRetries: Int = 5,
    private var maxHistoryCharacters: Int = 4000,
    val baseURL: String = "http://localhost:8080",
    val model: String = "gpt-3.5-turbo",
    var useHistory: Boolean = true,
) : WebSocketServer(resourceBase) {

    abstract fun hands(): java.util.Map<String, Object>
    abstract fun heart(hands: java.util.Map<String, Object>): Heart

    protected open val apiKey: String = File(File(System.getProperty("user.home")), "openai.key").readText().trim()

    open val api = OpenAIClient(apiKey)

    open val spinner =
        """<div class="spinner-border" role="status"><span class="sr-only">Loading...</span></div>"""

    open val sessionDataStorage = SessionDataStorage(File(File(".skynet"), applicationName))

    override fun configure(context: WebAppContext) {
        super.configure(context)

        if (null != oauthConfig) (object : AuthenticatedWebsite() {
            override val redirectUri = "$baseURL/oauth2callback"
            override val applicationName: String = this@SkyenetSessionServer.applicationName
            override fun getKey(): InputStream? {
                return FileUtils.openInputStream(File(oauthConfig))
            }
        }).configure(context)

        context.addServlet(
            ServletHolder(
                "yamlDescriptor",
                object : HttpServlet() {
                    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
                        resp.contentType = "text/plain"
                        resp.status = HttpServletResponse.SC_OK
                        val apiDescription = Brain.apiDescription(hands(), yamlDescriber)
                        resp.writer.write(apiDescription)
                    }
                }),
            "/yamlDescriptor"
        )

        context.addServlet(
            ServletHolder(
                "appInfo",
                object : HttpServlet() {
                    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
                        resp.contentType = "text/json"
                        resp.status = HttpServletResponse.SC_OK
                        resp.writer.write(
                            SessionDataStorage.objectMapper.writeValueAsString(
                                mapOf(
                                    "applicationName" to applicationName,
                                    "baseURL" to baseURL,
                                    "model" to model,
                                    "useHistory" to useHistory,
                                    "maxHistoryCharacters" to maxHistoryCharacters,
                                    "maxRetries" to maxRetries,
                                    "autoRun" to autoRun,
                                    "apiKey" to apiKey
                                )
                            )
                        )
                    }
                }),
            "/appInfo"
        )

        context.addServlet(
            ServletHolder(
                "sessionList",
                object : HttpServlet() {
                    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
                        resp.contentType = "text/html"
                        resp.status = HttpServletResponse.SC_OK
                        val links = sessionDataStorage.listSessions().joinToString("<br/>") {
                            """<a href="javascript:void(0)" onclick="window.location.href='/#$it';window.location.reload();">
                            |${sessionDataStorage.getSessionName(it)}
                            |</a><br/>""".trimMargin()
                        }
                        resp.writer.write(
                            """
                            |<html>
                            |<head>
                            |<title>Sessions</title>
                            |</head>
                            |<body>
                            |$links
                            |</body>
                            |</html>
                            """.trimMargin()
                        )
                    }
                }),
            "/sessions"
        )
    }

    override fun newSession(sessionId: String): SessionState {
        return SkyenetSession(sessionId)
    }

    open inner class SkyenetSession(sessionId: String) :
        SessionStateByID(sessionId, sessionDataStorage.loadMessages(sessionId)) {
        val hands = hands()
        val heart = heart(hands)
        val history: MutableMap<String, OperationStatus> by lazy {
            sessionDataStorage.loadOperations(sessionId)
        }
        open val brain by lazy {
            object : Brain(
                api = api,
                hands = hands,
                language = heart.getLanguage(),
                yamlDescriber = yamlDescriber,
                model = model
            ) {
                override fun getChatMessages(apiDescription: String) =
                    if (useHistory) {
                        super.getChatMessages(apiDescription) + getHistory(
                            history.values,
                            10,
                            maxHistoryCharacters
                        )
                    } else {
                        super.getChatMessages(apiDescription)
                    }

            }
        }

        override fun onWebSocketText(socket: MessageWebSocket, describedInstruction: String) {
            logger.debug("$sessionId - Received message: $describedInstruction")
            try {
                val opCmdPattern = """![a-z]{3,7},.*""".toRegex()
                if (opCmdPattern.matches(describedInstruction)) {
                    val id = describedInstruction.substring(1, describedInstruction.indexOf(","))
                    val code = describedInstruction.substring(id.length + 2)
                    history[id]?.onMessage(code)
                    sessionDataStorage.updateOperationStatus(sessionId, id, history[id]!!)
                } else {
                    Thread {
                        try {
                            run(describedInstruction)
                        } catch (e: Exception) {
                            logger.warn("$sessionId - Error processing message: $describedInstruction", e)
                        }
                    }.start()
                }
            } catch (e: Exception) {
                logger.warn("$sessionId - Error processing message: $describedInstruction", e)
            }
        }

        open fun run(
            describedInstruction: String,
        ) {
            OutputInterceptor.setupInterceptor()
            logger.debug("${sessionId} - Processing message: $describedInstruction")
            val operationID = (0..5).map { ('a'..'z').random() }.joinToString("")
            val status = OperationStatus(
                operationID = operationID,
                instruction = describedInstruction,
                thread = Thread.currentThread(),
            )
            history[operationID] = status
            sessionDataStorage.updateOperationStatus(sessionId, operationID, status)
            var retries = maxRetries
            var codedInstruction = ""
            var messageTrail = """$operationID,<button class="cancel-button" data-id="$operationID">&times;</button>"""
            val language = heart.getLanguage()
            if (describedInstruction.startsWith("!!!")) {
                codedInstruction = describedInstruction.substringAfter("!!!")
                retries = 0
                //language=HTML
                messageTrail += """<div><h3>Code:</h3><pre><code class="language-$language">$codedInstruction</code></pre></div>"""
            } else {
                //language=HTML
                messageTrail += """<div><h3>Command:</h3><pre>$describedInstruction</pre></div>"""
                //language=HTML
                val triple =
                    implement(messageTrail, describedInstruction, language, status)
                codedInstruction = triple.first
                messageTrail += triple.second
            }

            try {
                while (retries >= 0 && !status.cancelFlag.get()) {
                    try {
                        if (status.cancelFlag.get()) {
                            status.status = OperationStatus.OperationState.Cancelled
                            break
                        }
                        if (!autoRun) {
                            //language=HTML
                            send("""$messageTrail<div><button class="play-button" data-id="$operationID">▶</button></div>""")
                            logger.debug("${sessionId} - Waiting for run")
                            status.runSemaphore.acquire()
                            logger.debug("${sessionId} - Run received")
                        }
                        if (status.cancelFlag.get()) {
                            status.status = OperationStatus.OperationState.Cancelled
                            break
                        }
                        send("""$messageTrail<div>$spinner</div>""")
                        status.status = OperationStatus.OperationState.Running
                        sessionDataStorage.updateOperationStatus(sessionId, status.operationID, status)
                        messageTrail += execute(messageTrail, status, codedInstruction)
                        status.status = OperationStatus.OperationState.Complete
                        send(messageTrail)
                        break
                    } catch (e: Exception) {
                        logger.info("${sessionId} - Error", e)
                        //language=HTML
                        messageTrail += """<div><h3>Error:</h3><pre>${toString(e)}</pre></div>"""
                        status.status = OperationStatus.OperationState.Error
                        status.resultOutput = OutputInterceptor.getThreadOutput()
                        status.resultValue = toString(e)
                        sessionDataStorage.updateOperationStatus(sessionId, operationID, status)
                        if (retries <= 0 || status.cancelFlag.get()) {
                            //language=HTML
                            messageTrail += """<div><h3>Out of Retries!</h3></div>"""
                            logger.debug("${sessionId} - Out of retries")
                            this@SkyenetSession.send(messageTrail)
                            break
                        } else {
                            retries--
                            //language=HTML
                            val pair = fix(
                                messageTrail,
                                describedInstruction,
                                codedInstruction,
                                e,
                                language,
                                status
                            )
                            codedInstruction = pair.first
                            messageTrail += pair.second
                            status.status = OperationStatus.OperationState.Implemented
                            sessionDataStorage.updateOperationStatus(sessionId, status.operationID, status)
                        }
                    }
                }
            } catch (e: Exception) {
                //language=HTML
                messageTrail += """<div><h3>Error:</h3><pre>${e.message}</pre></div>"""
                logger.warn("${sessionId} - Error: ${e.message}")
                this@SkyenetSession.send(messageTrail)
            } finally {
                sessionDataStorage.updateOperationStatus(sessionId, operationID, status)
            }
        }

        override fun setMessage(key: String, value: String) {
            sessionDataStorage.updateMessage(sessionId, key, value)
            super.setMessage(key, value)
        }


        open fun implement(
            messageTrail: String,
            describedInstruction: String,
            language: String,
            status: OperationStatus,
        ): Triple<String, String, String> {
            //language=HTML
            send("""$messageTrail<div><h3>Code:</h3>$spinner</div>""")
            val response = brain.implement(describedInstruction)
            val codeBlocks = extractCodeBlocks(response)
            val renderedResponse = getRenderedResponse(Pair(response, codeBlocks))
            val codedInstruction = getCode(language, codeBlocks)
            logger.debug("$sessionId - Response: $renderedResponse")
            logger.debug("$sessionId - Code: $codedInstruction")
            status.responseText = renderedResponse
            status.responseCode = codedInstruction
            status.status = OperationStatus.OperationState.Implemented
            sessionDataStorage.updateOperationStatus(sessionId, status.operationID, status)
            //language=HTML
            return Triple(codedInstruction, """<div><h3>Code:</h3>${renderedResponse}</div>""", renderedResponse)
        }

        open fun fix(
            messageTrail: String,
            describedInstruction: String,
            codedInstruction: String,
            e: Exception,
            language: String,
            status: OperationStatus,
        ): Pair<String, String> {
            //language=HTML
            send("""$messageTrail<div><h3>New Code:</h3>$spinner</div>""")
            val respondWithCode =
                brain.fixCommand(describedInstruction, codedInstruction, e, status.resultOutput)
            val renderedResponse = getRenderedResponse(respondWithCode)
            val newCode = getCode(language, respondWithCode.second)
            logger.debug("$sessionId - Response: $renderedResponse")
            logger.debug("$sessionId - Code: $newCode")
            status.responseText = renderedResponse
            status.responseCode = newCode
            //language=HTML
            return Pair(newCode, """<div><h3>New Code:</h3>$renderedResponse</div>""")
        }

        open fun execute(
            messageTrail: String,
            status: OperationStatus,
            codedInstruction: String,
        ): String {
            //language=HTML
            logger.info("$sessionId - Running $codedInstruction")
            OutputInterceptor.clearThreadOutput()
            val result = heart.run(codedInstruction)
            logger.info("$sessionId - Result: $result")
            status.resultValue = result.toString()
            status.resultOutput = OutputInterceptor.getThreadOutput()
            //language=HTML
            return """<div><h3>Output:</h3><pre>${OutputInterceptor.getThreadOutput()}</pre><h3>Returns:</h3><pre>${result}</pre></div>"""
        }
    }

    open fun toString(e: Throwable): String {
        val sw = java.io.StringWriter()
        e.printStackTrace(java.io.PrintWriter(sw))
        return sw.toString()
    }

    companion object {
        val logger = org.slf4j.LoggerFactory.getLogger(SkyenetSessionServer::class.java)
    }

}

