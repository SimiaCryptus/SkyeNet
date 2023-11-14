package com.simiacryptus.skyenet.session

abstract class SessionDiv {
    abstract fun append(htmlToAppend: String, showSpinner: Boolean) : Unit
    abstract fun sessionID(): String
    abstract fun divID(): String
}