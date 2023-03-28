package com.simiacryptus.skyenet

import AutoScript
import org.junit.jupiter.api.Test

class Test {

    @Test
    fun testAutoScript() {
        AutoScript(arrayOf()).run("println(\"Hello World\")")
    }

    @Test
    fun testSpeak() {
        try {
            AutoScript(arrayOf("import com.simiacryptus.skyenet.Fingers._")).run("""
            speak("Hello World");
            """.trimIndent())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Test
    fun testSpeak_Direct() {
        Fingers.speak(
            """
        ...Hello World!...
        """.trimIndent()
        );
    }
    @Test
    fun testEars() {
        Ears.startExecutionListener()
    }

    @Test
    fun testHands() {
        Hands.command("Find the largest prime number less than 1000 and show it in a dialog box.")
    }
    @Test
    fun testPOA() {
        Hands.command("Speak the Pledge of Allegiance.")
    }
}