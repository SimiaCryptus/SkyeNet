package com.simiacryptus.skyenet.webui.session

import com.simiacryptus.skyenet.core.platform.Session

abstract class SessionMessage {
    abstract fun append(htmlToAppend: String, showSpinner: Boolean = true) : Unit
    fun complete(htmlToAppend: String) : Unit = append(htmlToAppend, false)
    abstract fun sessionID(): Session
    abstract fun divID(): String
}