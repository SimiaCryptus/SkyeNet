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
            yamlDescriber = parent.yamlDescriber,
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
        describedInstruction: String,
    ) {
        OutputInterceptor.setupInterceptor()
        SkyenetCodingSessionServer.logger.debug("${sessionId} - Processing message: $describedInstruction")
        val operationID = (0..5).map { ('a'..'z').random() }.joinToString("")
        val status = OperationStatus(
            operationID = operationID,
            instruction = describedInstruction,
            thread = Thread.currentThread(),
        )
        history[operationID] = status
        parent.sessionDataStorage.updateOperationStatus(sessionId, operationID, status)
        var retries = parent.maxRetries
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
                    if (!parent.autoRun) {
                        //language=HTML
                        send("""$messageTrail<div><button class="play-button" data-id="$operationID">▶</button></div>""")
                        SkyenetCodingSessionServer.logger.debug("${sessionId} - Waiting for run")
                        status.runSemaphore.acquire()
                        SkyenetCodingSessionServer.logger.debug("${sessionId} - Run received")
                    }
                    if (status.cancelFlag.get()) {
                        status.status = OperationStatus.OperationState.Cancelled
                        break
                    }
                    send("""$messageTrail<div>${parent.spinner}</div>""")
                    status.status = OperationStatus.OperationState.Running
                    parent.sessionDataStorage.updateOperationStatus(sessionId, status.operationID, status)
                    messageTrail += execute(messageTrail, status, codedInstruction)
                    status.status = OperationStatus.OperationState.Complete
                    send(messageTrail)
                    break
                } catch (e: Exception) {
                    SkyenetCodingSessionServer.logger.info("${sessionId} - Error", e)
                    //language=HTML
                    messageTrail += """<div><h3>Error:</h3><pre>${parent.toString(e)}</pre></div>"""
                    status.status = OperationStatus.OperationState.Error
                    status.resultOutput = OutputInterceptor.getThreadOutput()
                    status.resultValue = parent.toString(e)
                    parent.sessionDataStorage.updateOperationStatus(sessionId, operationID, status)
                    if (retries <= 0 || status.cancelFlag.get()) {
                        //language=HTML
                        messageTrail += """<div><h3>Out of Retries!</h3></div>"""
                        SkyenetCodingSessionServer.logger.debug("${sessionId} - Out of retries")
                        this@SkyenetCodingSession.send(messageTrail)
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
                        parent.sessionDataStorage.updateOperationStatus(sessionId, status.operationID, status)
                    }
                }
            }
        } catch (e: Exception) {
            //language=HTML
            messageTrail += """<div><h3>Error:</h3><pre>${e.message}</pre></div>"""
            SkyenetCodingSessionServer.logger.warn("${sessionId} - Error: ${e.message}")
            this@SkyenetCodingSession.send(messageTrail)
        } finally {
            parent.sessionDataStorage.updateOperationStatus(sessionId, operationID, status)
        }
    }

    override fun setMessage(key: String, value: String) {
        parent.sessionDataStorage.updateMessage(sessionId, key, value)
        super.setMessage(key, value)
    }


    open fun implement(
        messageTrail: String,
        describedInstruction: String,
        language: String,
        status: OperationStatus,
    ): Triple<String, String, String> {
        //language=HTML
        send("""$messageTrail<div><h3>Code:</h3>${parent.spinner}</div>""")
        val response = brain.implement(describedInstruction)
        val codeBlocks = Brain.extractCodeBlocks(response)
        val renderedResponse = SessionServerUtil.getRenderedResponse(Pair(response, codeBlocks))
        val codedInstruction = SessionServerUtil.getCode(language, codeBlocks)
        SkyenetCodingSessionServer.logger.debug("$sessionId - Response: $renderedResponse")
        SkyenetCodingSessionServer.logger.debug("$sessionId - Code: $codedInstruction")
        status.responseText = renderedResponse
        status.responseCode = codedInstruction
        status.status = OperationStatus.OperationState.Implemented
        parent.sessionDataStorage.updateOperationStatus(sessionId, status.operationID, status)
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
        send("""$messageTrail<div><h3>New Code:</h3>${parent.spinner}</div>""")
        val respondWithCode =
            brain.fixCommand(describedInstruction, codedInstruction, e, status.resultOutput)
        val renderedResponse = SessionServerUtil.getRenderedResponse(respondWithCode)
        val newCode = SessionServerUtil.getCode(language, respondWithCode.second)
        SkyenetCodingSessionServer.logger.debug("$sessionId - Response: $renderedResponse")
        SkyenetCodingSessionServer.logger.debug("$sessionId - Code: $newCode")
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
        SkyenetCodingSessionServer.logger.info("$sessionId - Running $codedInstruction")
        OutputInterceptor.clearThreadOutput()
        val result = heart.run(codedInstruction)
        SkyenetCodingSessionServer.logger.info("$sessionId - Result: $result")
        status.resultValue = result.toString()
        status.resultOutput = OutputInterceptor.getThreadOutput()
        //language=HTML
        return """<div><h3>Output:</h3><pre>${OutputInterceptor.getThreadOutput()}</pre><h3>Returns:</h3><pre>${result}</pre></div>"""
    }
}