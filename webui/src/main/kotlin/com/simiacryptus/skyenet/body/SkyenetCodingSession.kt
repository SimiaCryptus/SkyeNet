package com.simiacryptus.skyenet.body

import com.simiacryptus.skyenet.Brain
import com.simiacryptus.skyenet.OutputInterceptor

open class SkyenetCodingSession(
    sessionId: String,
    private val parent: SkyenetCodingSessionServer,
) : PersistentSessionBase(sessionId, parent.sessionDataStorage) {

    val history: MutableMap<String, OperationStatus> by lazy {
        parent.sessionDataStorage.loadOperations(sessionId)
    }

    override fun onCmd(id: String, code: String) {
        history[id]?.onMessage(code)
        parent.sessionDataStorage.updateOperationStatus(sessionId, id, history[id]!!)
        super.onCmd(id, code)
    }

    val hands = parent.hands()
    val heart = parent.heart(hands)
    open val brain by lazy {
        object : Brain(
            api = parent.api,
            hands = hands,
            language = heart.getLanguage(),
            yamlDescriber = parent.typeDescriber,
            model = parent.model
        ) {
            override fun getChatMessages(apiDescription: String) =
                if (parent.useHistory) {
                    super.getChatMessages(apiDescription) + SessionServerUtil.getHistory(
                        history.values,
                        10,
                        parent.maxHistoryCharacters
                    )
                } else {
                    super.getChatMessages(apiDescription)
                }

        }
    }

    override fun run(
        userMessage: String,
    ) {
        OutputInterceptor.setupInterceptor()
        logger.debug("${sessionId} - Processing message: $userMessage")
        val operationID = (0..5).map { ('a'..'z').random() }.joinToString("")
        val status = OperationStatus(
            operationID = operationID,
            instruction = userMessage,
            thread = Thread.currentThread(),
        )
        history[operationID] = status
        parent.sessionDataStorage.updateOperationStatus(sessionId, operationID, status)
        var retries = parent.maxRetries
        var codedInstruction: String
        var messageTrail = """$operationID,<button class="cancel-button" data-id="$operationID">&times;</button>"""
        val language = heart.getLanguage()
        if (userMessage.startsWith("!!!")) {
            codedInstruction = userMessage.substringAfter("!!!")
            retries = 0
            //language=HTML
            messageTrail += """<div><h3>Code:</h3><pre><code class="language-$language">$codedInstruction</code></pre></div>"""
        } else {
            //language=HTML
            messageTrail += """<div><h3>Command:</h3><pre>$userMessage</pre></div>"""
            //language=HTML
            val triple =
                implement(messageTrail, userMessage, language, status)
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
                    if (!parent.autoRun) {
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
                    send("""$messageTrail<div>${SkyenetSessionServerBase.spinner}</div>""")
                    status.status = OperationStatus.OperationState.Running
                    parent.sessionDataStorage.updateOperationStatus(sessionId, status.operationID, status)
                    messageTrail += execute(messageTrail, status, codedInstruction)
                    status.status = OperationStatus.OperationState.Complete
                    send(messageTrail)
                    break
                } catch (e: Exception) {
                    logger.info("${sessionId} - Error", e)
                    //language=HTML
                    messageTrail += """<div><h3>Error:</h3><pre>${parent.toString(e)}</pre></div>"""
                    status.status = OperationStatus.OperationState.Error
                    status.resultOutput = OutputInterceptor.getGlobalOutput()
                    status.resultValue = parent.toString(e)
                    parent.sessionDataStorage.updateOperationStatus(sessionId, operationID, status)
                    if (retries <= 0 || status.cancelFlag.get()) {
                        //language=HTML
                        messageTrail += """<div><h3>Out of Retries!</h3></div>"""
                        logger.debug("${sessionId} - Out of retries")
                        this@SkyenetCodingSession.send(messageTrail)
                        break
                    } else {
                        retries--
                        //language=HTML
                        val pair = fix(
                            messageTrail,
                            userMessage,
                            codedInstruction,
                            e,
                            language,
                            status
                        )
                        codedInstruction = pair.first
                        messageTrail += pair.second
                        status.status = OperationStatus.OperationState.Implemented
                        parent.sessionDataStorage.updateOperationStatus(sessionId, status.operationID, status)
                    }
                }
            }
        } catch (e: Exception) {
            //language=HTML
            messageTrail += """<div><h3>Error:</h3><pre>${e.message}</pre></div>"""
            logger.warn("${sessionId} - Error: ${e.message}")
            this@SkyenetCodingSession.send(messageTrail)
        } finally {
            parent.sessionDataStorage.updateOperationStatus(sessionId, operationID, status)
        }
    }

    override fun setMessage(key: String, value: String) : Int {
        parent.sessionDataStorage.updateMessage(sessionId, key, value)
        return super.setMessage(key, value)
    }


    open fun implement(
        messageTrail: String,
        describedInstruction: String,
        language: String,
        status: OperationStatus,
    ): Triple<String, String, String> {
        //language=HTML
        val buffer = StringBuffer()
        buffer.append("""<div><h3>Code:</h3>""")
        send("""$messageTrail$buffer${SkyenetSessionServerBase.spinner}</div>""")
        val response = brain.implement(describedInstruction)
        val codeBlocks = Brain.extractCodeBlocks(response)
        var renderedResponse = SessionServerUtil.getRenderedResponse(codeBlocks)
        var codedInstruction = SessionServerUtil.getCode(language, codeBlocks)
        logger.debug("$sessionId - Response: $renderedResponse")
        logger.debug("$sessionId - Code: $codedInstruction")
        status.responseText = renderedResponse
        status.responseCode = codedInstruction
        buffer.append("""<div>${renderedResponse}</div>""")

        for (int in 0..3) {
            try {
                heart.validate(codedInstruction)
                buffer.append("""</div>""")
                break
            } catch (ex: Throwable) {
                buffer.append("""<pre><code class="language-$language">${codedInstruction}</code></pre><pre>${ex.message}</pre>""")
                send("""$messageTrail$buffer${SkyenetSessionServerBase.spinner}</div>""")
                val respondWithCode =
                    brain.fixCommand(describedInstruction, codedInstruction, ex, status.resultOutput)
                renderedResponse = SessionServerUtil.getRenderedResponse(respondWithCode.second)
                codedInstruction = SessionServerUtil.getCode(language, respondWithCode.second)
                buffer.append("""<div>${renderedResponse}</div>""")
                logger.debug("$sessionId - Response: $renderedResponse")
                logger.debug("$sessionId - Code: $codedInstruction")
            }
        }
        status.status = OperationStatus.OperationState.Implemented
        parent.sessionDataStorage.updateOperationStatus(sessionId, status.operationID, status)
        //language=HTML
        return Triple(codedInstruction, buffer.toString(), renderedResponse)
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
        send("""$messageTrail<div><h3>New Code:</h3>${SkyenetSessionServerBase.spinner}</div>""")
        val respondWithCode =
            brain.fixCommand(describedInstruction, codedInstruction, e, status.resultOutput)
        val renderedResponse = SessionServerUtil.getRenderedResponse(respondWithCode.second)
        val newCode = SessionServerUtil.getCode(language, respondWithCode.second)
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
        OutputInterceptor.clearGlobalOutput()
        val result = heart.run(codedInstruction)
        logger.info("$sessionId - Result: $result")
        status.resultValue = result.toString()
        val output = OutputInterceptor.getGlobalOutput()
        status.resultOutput = output
        //language=HTML
        return """<div><h3>Output:</h3><pre>$output</pre><h3>Returns:</h3><pre>${result}</pre></div>"""
    }
    companion object {
        val logger = org.slf4j.LoggerFactory.getLogger(SkyenetCodingSessionServer::class.java)
    }
}