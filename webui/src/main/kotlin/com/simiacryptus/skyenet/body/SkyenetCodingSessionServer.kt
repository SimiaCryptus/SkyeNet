package com.simiacryptus.skyenet.body

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

abstract class SkyenetCodingSessionServer(
    override val applicationName: String,
    val yamlDescriber: YamlDescriber = YamlDescriber(),
    override val oauthConfig: String? = null,
    val autoRun: Boolean = false,
    resourceBase: String = "simpleSession",
    val maxRetries: Int = 5,
    var maxHistoryCharacters: Int = 4000,
    override val baseURL: String = "http://localhost:8080",
    val model: String = "gpt-3.5-turbo",
    var useHistory: Boolean = true,
) : SkyenetSessionServerBase(
    applicationName = applicationName,
    oauthConfig = oauthConfig,
    resourceBase = resourceBase,
    baseURL = baseURL,
) {

    abstract fun hands(): java.util.Map<String, Object>
    abstract fun heart(hands: java.util.Map<String, Object>): Heart

    override fun configure(context: WebAppContext) {
        super.configure(context)

        if (null != oauthConfig) (object : AuthenticatedWebsite() {
            override val redirectUri = "$baseURL/oauth2callback"
            override val applicationName: String = this@SkyenetCodingSessionServer.applicationName
            override fun getKey(): InputStream? {
                return FileUtils.openInputStream(File(oauthConfig))
            }
        }).configure(context)

        context.addServlet(yamlDescriptor, "/yamlDescriptor")
    }

    protected open val yamlDescriptor = ServletHolder(
        "yamlDescriptor",
        object : HttpServlet() {
            override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
                resp.contentType = "text/plain"
                resp.status = HttpServletResponse.SC_OK
                val apiDescription = Brain.apiDescription(hands(), yamlDescriber)
                resp.writer.write(apiDescription)
            }
        })

    override fun newSession(sessionId: String): SessionState {
        return SkyenetSession(sessionId)
    }

    open inner class SkyenetSession(sessionId: String) :
        SkyenetSessionBase(sessionId) {
        val hands = hands()
        val heart = heart(hands)
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

        override fun run(
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

    companion object {
        val logger = org.slf4j.LoggerFactory.getLogger(SkyenetCodingSessionServer::class.java)
    }

}

