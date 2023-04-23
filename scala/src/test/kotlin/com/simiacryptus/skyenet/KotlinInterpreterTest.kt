//package com.simiacryptus.skyenet
//
//import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
//import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineBase
//import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
//import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
//import org.jetbrains.kotlin.config.CommonConfigurationKeys
//import org.jetbrains.kotlin.config.CompilerConfiguration
//import java.lang.reflect.Proxy
//import javax.script.ScriptEngineManager
//
//object KotlinInterpreterTest {
//
//    interface Foo {
//        fun bar(): String
//    }
//
//    object SysTool {
//        fun print(text: String) {
//            println(text)
//        }
//    }
//
//    class ProxyWrapper(private val proxy: Foo) : Foo {
//        override fun bar(): String {
//            return proxy.bar()
//        }
//    }
//
//    fun prevalidateKotlin(script: String, engine: KotlinJsr223JvmScriptEngineBase): Boolean {
//        return try {
//            engine.compile(script, engine.context)
//            true
//        } catch (e: Throwable) {
//            println("Compilation failed: ${e.message}")
//            false
//        }
//    }
//
//    @JvmStatic
//    fun main(args: Array<String>) {
//        val scriptEngineManager = ScriptEngineManager(this::class.java.classLoader)
//        scriptEngineManager.engineFactories.forEach {
//            println("Engine: ${it.engineName} ${it.engineVersion} ${it.languageName} ${it.languageVersion}")
//        }
//        val engine = scriptEngineManager.getEngineByExtension("kts")
//        fun run(msg: String) {
//            if (!prevalidateKotlin(msg, engine as KotlinJsr223JvmScriptEngineBase)) {
//                return
//            }
//            engine.eval(
//                msg
//            )
//        }
//
//        engine.put("sys", SysTool)
//        run(
//            """
//            sys.print("Hello World")
//            """.trimIndent()
//        )
//
//        var gptTools = Proxy.newProxyInstance(Foo::class.java.classLoader, arrayOf(Foo::class.java)) { _, method, _ ->
//            when (method.name) {
//                "bar" -> "Foo says Hello World"
//                else -> throw IllegalArgumentException("Unknown method: ${method.name}")
//            }
//        } as Foo
//        // Needed workaround
//        gptTools = ProxyWrapper(gptTools)
//        engine.put("tool", gptTools)
//        run(
//            """
//            sys.print("Hello World Again")
//            """.trimIndent()
//        )
//        run(
//            """
//            println(tool.bar())
//            """.trimIndent()
//        )
//    }
//
//}