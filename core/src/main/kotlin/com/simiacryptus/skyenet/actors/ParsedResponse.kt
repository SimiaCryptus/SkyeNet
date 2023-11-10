package com.simiacryptus.skyenet.actors

interface ParsedResponse<T> {
    fun getText(): String
    fun getObj(): T
}