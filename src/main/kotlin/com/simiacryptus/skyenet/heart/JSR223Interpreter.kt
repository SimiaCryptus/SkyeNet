@file:Suppress("unused")

package com.simiacryptus.skyenet.heart

import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineBase
import javax.script.ScriptEngineManager

open class JSR223Interpreter(
    private val prefix: String = "",
    defs: Map<String, Any> = mapOf(),
    extension: String = "kts",
) {
    private val classLoader = this::class.java.classLoader

    private val engine = ScriptEngineManager(classLoader).getEngineByExtension(extension)

    init {
        defs.forEach { (key, value) ->
            engine.put(key, value)
        }
    }

    fun run(code: String): Any? {
        return engine.eval("$prefix\n$code")
    }

    fun validate(code: String) : Exception? {
        return try {
            if(engine is KotlinJsr223JvmScriptEngineBase) {
                engine.compile(code, engine.context)
                null
            } else {
                null
            }
        } catch (e: Exception) {
            e
        }
    }

    companion object {
        fun prevalidateKotlin(script: String, engine: KotlinJsr223JvmScriptEngineBase): Exception? {
            return try {
                engine.compile(script, engine.context)
                null
            } catch (e: Exception) {
                e
            }
        }

    }
}