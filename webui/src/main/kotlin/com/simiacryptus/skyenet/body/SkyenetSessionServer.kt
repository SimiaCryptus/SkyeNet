package com.simiacryptus.skyenet.body

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.Brain
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

    private val spinner =
        """<div class="spinner-border" role="status"><span class="sr-only">Loading...</span></div>"""

    val sessionDataStorage = SessionDataStorage(File(File(".skynet"), applicationName))

    override fun configure(context: WebAppContext) {
        super.configure(context)

        if (null != oauthConfig) object : AuthenticatedWebsite() {
            override val redirectUri = "$baseURL/oauth2callback"
            override val applicationName: String = this@SkyenetSessionServer.applicationName
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
                            """<a href="javascript:void(0)" onclick="window.location.href='/#$it';window.location.reload();">${
                                sessionDataStorage.getSessionName(
                                    it
                                )
                            }</a><br/>"""
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

    inner class SkyenetSession(sessionId: String) :
        SessionStateByID(sessionId, sessionDataStorage.loadMessages(sessionId)) {
        val hands = hands()
        val heart = heart(hands)
        val history: MutableMap<String, OperationStatus> by lazy {
            sessionDataStorage.loadOperations(sessionId)
        }
        val brain by lazy {
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

        open fun SessionStateByID.run(
            describedInstruction: String,
        ) {
            OutputInterceptor.setupInterceptor()
            logger.debug("$sessionId - Processing message: $describedInstruction")
            val operationID = (0..5).map { ('a'..'z').random() }.joinToString("")
            val status = OperationStatus(
                operationID = operationID,
                instruction = describedInstruction,
                thread = Thread.currentThread(),
            )
            history[operationID] = status
            sessionDataStorage.updateOperationStatus(sessionId, operationID, status)
            var retries = maxRetries
            var renderedResponse = ""
            var codedInstruction = ""
            var messageTrail = """$operationID,
            |<button class="cancel-button" data-id="$operationID">&times;</button>
        """.trimMargin()
            val language = heart.getLanguage()
            if (describedInstruction.startsWith("!!!")) {
                codedInstruction = describedInstruction.substringAfter("!!!")
                retries = 0
                //language=HTML
                messageTrail += """
                    |<div>
                    |<h3>Code:</h3>
                    |<pre><code class="language-$language">
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
                val triple =
                    implement(messageTrail, describedInstruction, renderedResponse, codedInstruction, language, status)
                codedInstruction = triple.first
                messageTrail = triple.second
                renderedResponse = triple.third
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
                        if (status.cancelFlag.get()) {
                            status.status = OperationStatus.OperationState.Complete
                            sessionDataStorage.updateOperationStatus(sessionId, operationID, status)
                            break
                        }
                        if (!autoRun) {
                            logger.debug("$sessionId - Waiting for run")
                            status.runSemaphore.acquire()
                            logger.debug("$sessionId - Run received")
                        }
                        if (status.cancelFlag.get()) {
                            status.status = OperationStatus.OperationState.Complete
                            sessionDataStorage.updateOperationStatus(sessionId, operationID, status)
                            logger.debug("$sessionId - Cancelled")
                            break
                        }
                        messageTrail = execute(messageTrail, status, codedInstruction)
                        break
                    } catch (e: Exception) {
                        logger.info("$sessionId - Error", e)
                        //language=HTML
                        messageTrail += """
                                |<div>
                                |<h3>Error:</h3>
                                |<pre>
                                |${toString(e)}
                                |</pre>
                                |</div>
                                """.trimMargin().trim()
                        status.status = OperationStatus.OperationState.Error
                        status.resultOutput = OutputInterceptor.getThreadOutput()
                        status.resultValue = toString(e)
                        sessionDataStorage.updateOperationStatus(sessionId, operationID, status)
                        if (retries <= 0 || status.cancelFlag.get()) {
                            messageTrail += """
                                |<div>
                                |<h3>Out of Retries!</h3>
                                |</div>
                                """.trimMargin().trim()
                            logger.debug("$sessionId - Out of retries")
                            send(messageTrail)
                            break
                        } else {
                            retries--
                            //language=HTML
                            val pair = fix(
                                messageTrail,
                                describedInstruction,
                                codedInstruction,
                                e,
                                renderedResponse,
                                language,
                                status
                            )
                            codedInstruction = pair.first
                            messageTrail = pair.second
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
                logger.warn("$sessionId - Error: ${e.message}")
                send(messageTrail)
            }
        }

        override fun setMessage(key: String, value: String) {
            sessionDataStorage.updateMessage(sessionId, key, value)
            super.setMessage(key, value)
        }

        private fun implement(
            messageTrail: String,
            describedInstruction: String,
            renderedResponse: String,
            codedInstruction: String,
            language: String,
            status: OperationStatus,
        ): Triple<String, String, String> {
            var messageTrail1 = messageTrail
            var renderedResponse1 = renderedResponse
            var codedInstruction1 = codedInstruction
            send(
                """$messageTrail1
                |<div>
                |<h3>Code:</h3>
                |$spinner
                |</div>
                |""".trimMargin().trim()
            )
            var respondWithCode = brain.respondWithCode(describedInstruction)
            renderedResponse1 = getRenderedResponse(respondWithCode)
            codedInstruction1 = getCode(language, respondWithCode.second)
            logger.debug("$sessionId - Response: $renderedResponse1")
            logger.debug("$sessionId - Code: $codedInstruction1")
            status.responseText = renderedResponse1
            status.responseCode = codedInstruction1
            status.status = OperationStatus.OperationState.Implemented
            sessionDataStorage.updateOperationStatus(sessionId, status.operationID, status)

            //language=HTML
            messageTrail1 += """
                |<div>
                |<h3>Code:</h3>
                |${renderedResponse1}
                |</div>
            """.trimMargin().trim()
            return Triple(codedInstruction1, messageTrail1, renderedResponse1)
        }

        private fun fix(
            messageTrail: String,
            describedInstruction: String,
            codedInstruction: String,
            e: Exception,
            renderedResponse: String,
            language: String,
            status: OperationStatus,
        ): Pair<String, String> {
            var messageTrail1 = messageTrail
            var codedInstruction1 = codedInstruction
            var renderedResponse1 = renderedResponse
            send(
                """
                |$messageTrail1
                |<div>
                |<h3>New Code:</h3>
                |$spinner
                |</div>
                |""".trimMargin().trim()
            )
            val respondWithCode =
                brain.fixCommand(describedInstruction, codedInstruction1, e, OutputInterceptor.getThreadOutput())
            renderedResponse1 = getRenderedResponse(respondWithCode)
            codedInstruction1 = getCode(language, respondWithCode.second)
            logger.debug("$sessionId - Response: $renderedResponse1")
            logger.debug("$sessionId - Code: $codedInstruction1")
            status.responseText = renderedResponse1
            status.resultOutput = OutputInterceptor.getThreadOutput()
            status.responseCode = codedInstruction1
            status.status = OperationStatus.OperationState.Implemented
            sessionDataStorage.updateOperationStatus(sessionId, status.operationID, status)
            //language=HTML
            messageTrail1 += """
                |<div>
                |<h3>New Code:</h3>
                |$renderedResponse1
                |</div>
            """.trimMargin().trim()
            return Pair(codedInstruction1, messageTrail1)
        }

        private fun execute(
            messageTrail: String,
            status: OperationStatus,
            codedInstruction: String,
        ): String {
            var messageTrail1 = messageTrail
            send(
                """
                    |$messageTrail1
                    |<div>
                    |$spinner
                    |</div>
                """.trimMargin().trim()
            )
            status.status = OperationStatus.OperationState.Running
            sessionDataStorage.updateOperationStatus(sessionId, status.operationID, status)
            logger.debug("$sessionId - Running $codedInstruction")
            OutputInterceptor.clearThreadOutput()
            val result = heart.run(codedInstruction)
            logger.debug("$sessionId - Result: $result")
            val output = OutputInterceptor.getThreadOutput()
            status.resultValue = result.toString()
            status.resultOutput = output
            status.status = OperationStatus.OperationState.Complete
            sessionDataStorage.updateOperationStatus(sessionId, status.operationID, status)
            //language=HTML
            messageTrail1 += """
                |<div>
                |<h3>Output:</h3>
                |<pre>
                |$output
                |</pre>
                |
                |<h3>Returns:</h3>
                |<pre>
                |${result}
                |</pre>
                |</div>
                """.trimMargin().trim()
            send(messageTrail1)
            return messageTrail1
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

