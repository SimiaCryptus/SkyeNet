package com.simiacryptus.skyenet.session

import com.simiacryptus.skyenet.platform.SessionID

abstract class SessionDiv {
    abstract fun append(htmlToAppend: String, showSpinner: Boolean) : Unit
    abstract fun sessionID(): SessionID
    abstract fun divID(): String
}