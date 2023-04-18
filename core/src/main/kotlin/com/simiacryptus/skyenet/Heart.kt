package com.simiacryptus.skyenet

interface Heart {

    fun getLanguage(): String
    fun run(code: String): Any?
    fun validate(code: String): Exception?

    fun wrapCode(code: String): String = code
    fun <T : Any> wrapExecution(fn: () -> T?): T? = fn()
}