package com.simiacryptus.skyenet

import kotlinx.coroutines.runBlocking

object Fingers {
    @JvmStatic
    fun speak(text: String) {
        runBlocking {
            Mouth.synthesizeAndPlay(text)
        }
    }
}