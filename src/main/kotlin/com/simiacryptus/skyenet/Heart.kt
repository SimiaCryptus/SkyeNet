package com.simiacryptus.skyenet

import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value

/**
 * The heart is the interface to the GraalVM JavaScript engine for the SkyeNet system
 * It powers the body.
 */
class Heart(
    private val prefix: String = "",
    defs: Map<String, Any> = mapOf(),
) {
    private val engine = org.graalvm.polyglot.Context.newBuilder().build()
    init {
        defs.forEach { (key, value) ->
            engine.polyglotBindings.putMember(key, value)
            engine.getBindings("js").putMember(key, value)
        }
    }
    fun run(jsCode: String): Value {
        val source = Source.create("js", prefix + "\n" + jsCode)
        return engine.eval(source)
    }
}