package com.simiacryptus.skyenet.heart

import com.simiacryptus.skyenet.Heart
import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngine
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory
import org.jetbrains.kotlin.script.jsr223.KotlinStandardJsr223ScriptTemplate
import java.io.File
import java.lang.ref.WeakReference
import java.util.*
import javax.script.Bindings
import javax.script.CompiledScript
import javax.script.ScriptContext
import kotlin.reflect.KClass

open class KotlinLocalInterpreter(
    val defs: java.util.Map<String, Object> = HashMap<String, Object>() as java.util.Map<String, Object>
) : Heart {

    private val engine: KotlinJsr223JvmLocalScriptEngine

    init {
        System.setProperty("idea.io.use.nio2","true")
        val factory = KotlinJsr223JvmLocalScriptEngineFactory()
        engine = factory.scriptEngine as KotlinJsr223JvmLocalScriptEngine
        val bindings: Bindings? = engine.getBindings(ScriptContext.GLOBAL_SCOPE)
        defs.entrySet().forEach { (key, value) ->
            engine.put(key, value)
            bindings?.put(key, value)
        }
    }

    override fun validate(code: String): Exception? {
        val compile = engine.compile(wrapCode(code), engine.context)
        return null
    }

    override fun run(code: String): Any? {
        return wrapExecution {
            val wrapCode = wrapCode(code)
            println(
                """Running:
                |   ${wrapCode.trimIndent().replace("\n", "\n\t")}
                |""".trimMargin()
            )
            val context: ScriptContext = engine.context
            val compile: CompiledScript = engine.compile(wrapCode, context)
            val eval = compile.eval(context)
            eval
        }
    }

    override fun wrapCode(code: String): String {
        val out = ArrayList<String>()
        val (imports, otherCode) = code.split("\n").partition { it.startsWith("import ") }
        out.addAll(imports)
        defs.entrySet().forEach { (key, value) ->
            val uuid = storageMap.getOrPut(value) { UUID.randomUUID() }
            retrievalIndex.put(uuid, WeakReference(value))
            val fqClassName = KotlinLocalInterpreter.javaClass.name.replace("$", ".")
            val typeStr = typeOf(value)
            out.add("val $key : $typeStr = $fqClassName.retrievalIndex.get(java.util.UUID.fromString(\"$uuid\"))?.get()!! as $typeStr\n")
        }
        out.addAll(otherCode)
        print(out.joinToString("\n"))
        return super.wrapCode(out.joinToString("\n"))
    }

    open fun typeOf(value: Object?): String {
        if (value is java.lang.reflect.Proxy) {
            return value.javaClass.interfaces[0].name.replace("$", ".") + "?"
        }
        val replace = value?.javaClass?.name?.replace("$", ".")
        return if (replace != null) ("$replace") else "null"
    }

    companion object {
        val storageMap = WeakHashMap<Object, UUID>()
        val retrievalIndex = HashMap<UUID, WeakReference<Object>>()
    }

    override fun getLanguage(): String {
        return "Kotlin"
    }

}