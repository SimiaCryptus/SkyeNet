package com.simiacryptus.skyenet.core.actors

abstract class ParsedResponse<T>(val clazz: Class<T>) {
    abstract val text: String
    abstract val obj: T
    override fun toString() = text
}