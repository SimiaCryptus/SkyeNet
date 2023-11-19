package com.simiacryptus.skyenet.util

import com.simiacryptus.skyenet.ApplicationBase
import com.simiacryptus.skyenet.session.SessionBase
import java.util.function.Consumer

class HtmlTools(
    val app: ApplicationBase.ApplicationSession,
    private val operationID: String = SessionBase.randomID()
) {
    val spinner: String get() = """<div>${ApplicationBase.spinner}</div>"""
    val playButton: String get() = """<button class="play-button" data-id="$operationID">▶</button>"""
    val cancelButton: String get() = """<button class="cancel-button" data-id="$operationID">&times;</button>"""
    val regenButton: String get() = """<button class="regen-button" data-id="$operationID">♲</button>"""

    private val txtTriggers = mutableMapOf<String, Consumer<String>>()
    fun hrefLink(linkText : String, classname: String = """href-link""", handler: Consumer<Unit>): String {
        val operationID = SessionBase.randomID()
        app.linkTriggers[operationID] = handler
        return """<a class="$classname" data-id="$operationID">$linkText</a>"""
    }
    fun textInput(handler: Consumer<String>): String {
        val operationID = SessionBase.randomID()
        txtTriggers[operationID] = handler
        //language=HTML
        return """<form class="reply-form">
                       <textarea class="reply-input" data-id="$operationID" rows="3" placeholder="Type a message"></textarea>
                       <button class="text-submit-button" data-id="$operationID">Send</button>
                   </form>""".trimIndent()
    }
}