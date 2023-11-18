package com.simiacryptus.skyenet.actors

abstract class ParsedResponse<T>(val clazz: Class<T>) {
    abstract fun getText(): String
    abstract fun getObj(clazz: Class<T>): T
    fun getObj(): T = getObj(clazz)
}