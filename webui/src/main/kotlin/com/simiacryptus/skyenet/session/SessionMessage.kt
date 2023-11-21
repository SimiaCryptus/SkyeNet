package com.simiacryptus.skyenet.session

import com.simiacryptus.skyenet.platform.Session

abstract class SessionMessage {
    abstract fun append(htmlToAppend: String, showSpinner: Boolean) : Unit
    abstract fun sessionID(): Session
    abstract fun divID(): String
}