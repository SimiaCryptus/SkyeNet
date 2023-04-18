@file:Suppress("unused")

package com.simiacryptus.skyenet.heart

import com.simiacryptus.skyenet.Heart
import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineBase
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmDaemonLocalEvalScriptEngineFactory
import javax.script.ScriptEngine

open class KotlinDaemonInterpreter(
    defs: Map<String, Any> = mapOf(),
) : Heart {

    private val engine by lazy {
        System.setProperty("kotlin.script.classpath", System.getProperty("java.class.path"))
        val engine: ScriptEngine = KotlinJsr223JvmDaemonLocalEvalScriptEngineFactory().scriptEngine
        defs.forEach { (key, value) ->
            engine.put(key, value)
        }
        engine as KotlinJsr223JvmScriptEngineBase
    }

    override fun getLanguage(): String {
        return "kotlin"
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

