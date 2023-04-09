package com.simiacryptus.skyenet

//import org.graalvm.polyglot.HostAccess.Export

class TestTools(keyfile: String) {
    // Private details will not be exported
    private val mouth = Mouth(keyfile)
    // Export methods to be called from the script
//    @Export
    fun speak(text: String) = mouth.speak(text)
}
