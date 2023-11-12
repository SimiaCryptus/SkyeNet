package com.simiacryptus.skyenet.util

import com.simiacryptus.skyenet.sessions.ApplicationBase
import com.simiacryptus.skyenet.sessions.ChatSession

class HtmlTools(private val operationID: String = ChatSession.randomID()) {
    val spinner: String get() = """<div>${ApplicationBase.spinner}</div>"""
    val playButton: String get() = """<button class="play-button" data-id="$operationID">▶</button>"""
    val cancelButton: String get() = """<button class="cancel-button" data-id="$operationID">&times;</button>"""
    val regenButton: String get() = """<button class="regen-button" data-id="$operationID">♲</button>"""

    val linkTriggers = mutableMapOf<String, java.util.function.Consumer<Unit>>()

    private val txtTriggers = mutableMapOf<String, java.util.function.Consumer<String>>()
    fun hrefLink(handler: java.util.function.Consumer<Unit>): String {
        val operationID = ChatSession.randomID()
        linkTriggers[operationID] = handler
        return """<a class="href-link" data-id="$operationID">"""
    }
    fun textInput(handler: java.util.function.Consumer<String>): String {
        val operationID = ChatSession.randomID()
        txtTriggers[operationID] = handler
        //language=HTML
        return """<form class="reply-form">
                                   <textarea class="reply-input" data-id="$operationID" rows="3" placeholder="Type a message"></textarea>
                                   <button class="text-submit-button" data-id="$operationID">Send</button>
                               </form>""".trimIndent()
    }
}