package com.simiacryptus.skyenet.core.actors

abstract class ParsedResponse<T>(val clazz: Class<T>) {
    abstract val text: String
    abstract fun getObj(clazz: Class<T>): T
    val obj: T get() = getObj(clazz)
    override fun toString() = text
}