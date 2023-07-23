package com.simiacryptus.skyenet.body

import com.simiacryptus.openai.OpenAIClient
import org.apache.commons.io.FileUtils
import org.eclipse.jetty.webapp.WebAppContext
import java.io.File
import java.util.concurrent.Semaphore
import java.util.function.Consumer

abstract class SkyenetMacroChat(
    applicationName: String,
    baseURL: String,
    oauthConfig: String? = null,
    temperature: Double = 0.1,
) : SkyenetSessionServerBase(
    applicationName = applicationName,
    baseURL = baseURL,
    oauthConfig = oauthConfig,
    temperature = temperature,
) {
    override val api: OpenAIClient
        get() = OpenAIClient()

    override fun configure(context: WebAppContext) {
        super.configure(context)
        if (null != oauthConfig) AuthenticatedWebsite(
            "$baseURL/oauth2callback",
            this@SkyenetMacroChat.applicationName
        ) {
            FileUtils.openInputStream(File(oauthConfig))
        }.configure(context)
    }

    interface SessionUI {
        val spinner: String
        val playButton: String
        val cancelButton: String
        val regenButton: String
        fun hrefLink(handler: Consumer<Unit>): String
        fun textInput(handler: Consumer<String>): String
    }

    override fun newSession(sessionId: String): SessionInterface {
        val handler = MutableSessionHandler(null)
        val parent = this@SkyenetMacroChat

        val basicChatSession = object : PersistentSessionBase(
            sessionId = sessionId,
            parent.sessionDataStorage
        ) {
            val playSempaphores = mutableMapOf<String, Semaphore>()
            val threads = mutableMapOf<String, Thread>()
            val regenTriggers = mutableMapOf<String, Consumer<Unit>>()
            val linkTriggers = mutableMapOf<String, Consumer<Unit>>()
            val txtTriggers = mutableMapOf<String, Consumer<String>>()
            override fun run(userMessage: String) {
                val operationID = ChatSession.randomID()
                val thread = Thread {
                    playSempaphores[operationID] = Semaphore(0)
                    var responseContents = ChatSession.divInitializer(operationID)
                    try {
                        processMessage(userMessage, object : SessionUI {
                            override val spinner: String get() = """<div>${parent.spinner}</div>"""
                            override val playButton: String get() = """<button class="play-button" data-id="$operationID">▶</button>"""
                            override val cancelButton: String get() = """<button class="cancel-button" data-id="$operationID">&times;</button>"""
                            override val regenButton: String get() = """<button class="regen-button" data-id="$operationID">♲</button>"""

                            override fun hrefLink(handler:Consumer<Unit>): String {
                                val operationID = ChatSession.randomID()
                                linkTriggers[operationID] = handler
                                return """<a class="href-link" data-id="$operationID">"""
                            }

                            override fun textInput(handler:Consumer<String>): String {
                                val operationID = ChatSession.randomID()
                                txtTriggers[operationID] = handler
                                //language=HTML
                                return """<form class="reply-form">
                                    <textarea class="reply-input" data-id="$operationID" rows="3" placeholder="Type a message"></textarea>
                                    <button class="text-submit-button" data-id="$operationID">Send</button>
                                </form>""".trimIndent()
                            }

                        }) { message, showProgress ->
                            if (message.isNotBlank()) {
                                responseContents += """<div>$message</div>"""
                            }
                            val spinner = if (showProgress) """<div>${parent.spinner}</div>""" else ""
                            send("""$responseContents$spinner""")
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    } finally {
                        send(responseContents)
                    }
                }
                threads[operationID] = thread
                thread.start()
            }

            override fun onCmd(id: String, code: String) {
                if(code=="run") {
                    playSempaphores[id]?.release()
                }
                if(code=="cancel") {
                    threads[id]?.interrupt()
                }
                if(code=="regen") {
                    regenTriggers[id]?.accept(Unit)
                }
                if(code.startsWith("link")) {
                    linkTriggers[id]?.accept(Unit)
                }
                if(code.startsWith("userTxt,")) {
                    txtTriggers[id]?.accept(code.substring("userTxt,".length))
                }
                super.onCmd(id, code)
            }
        }
        handler.setDelegate(basicChatSession)
        return handler
    }

    abstract fun processMessage(
        userMessage: String,
        sessionUI: SessionUI,
        sendUpdate: (String, Boolean) -> Unit
    )

}

