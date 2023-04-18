package com.simiacryptus.skyenet.heart

import com.simiacryptus.skyenet.Heart
import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineBase
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory

open class KotlinLocalInterpreter(
    defs: Map<String, Any> = mapOf(),
) : Heart {

    private val engine: KotlinJsr223JvmScriptEngineBase

    init {
        val factory = KotlinJsr223JvmLocalScriptEngineFactory()
        engine = factory.getScriptEngine() as KotlinJsr223JvmScriptEngineBase
        defs.forEach { (key, value) ->
            engine.put(key, value)
        }
    }

    override fun getLanguage(): String {
        return "Kotlin"
    }

    override fun run(code: String): Any? {
        return wrapExecution { engine.eval(wrapCode(code)) }
    }

    override fun validate(code: String): Exception? {
        return try {
            engine.compile(wrapCode(code), engine.context)
            null
        } catch (e: Exception) {
            e
        }
    }
}