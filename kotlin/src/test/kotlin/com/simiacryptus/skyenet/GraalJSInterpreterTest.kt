package com.simiacryptus.skyenet

import java.lang.reflect.Proxy
import javax.script.ScriptEngineManager

object GraalJSInterpreterTest {

    interface Foo {
        fun bar(): String
    }

    object SysTool {
        fun print(text: String) {
            println(text)
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val scriptEngineManager = ScriptEngineManager(this::class.java.classLoader)
        scriptEngineManager.engineFactories.forEach {
            println("Engine: ${it.engineName} ${it.engineVersion} ${it.languageName} ${it.languageVersion}")
        }
        val engine = scriptEngineManager.getEngineByExtension("js")

        engine.put("sys", SysTool)
        engine.eval("""
            sys.print("Hello World")
            """.trimIndent())

        var gptTools = Proxy.newProxyInstance(Foo::class.java.classLoader, arrayOf(Foo::class.java)) { _, method, _ ->
            when (method.name) {
                "bar" -> "Foo says Hello World"
                else -> throw IllegalArgumentException("Unknown method: ${method.name}")
            }
        } as Foo
        engine.put("tool", gptTools)
        engine.eval("""
            sys.print("Hello World Again")
            """.trimIndent())
        engine.eval("""
            println(tool.bar())
            """.trimIndent())
    }
}