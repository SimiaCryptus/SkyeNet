package com.simiacryptus.skyenet.heart

import com.simiacryptus.skyenet.Heart
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory
import java.lang.ref.WeakReference
import java.util.*
import javax.script.Bindings
import javax.script.CompiledScript
import javax.script.ScriptContext

open class KotlinLocalInterpreter(
    val defs: java.util.Map<String, Object> = java.util.HashMap<String, Object>() as java.util.Map<String, Object>
) : Heart {

    private val engine: org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngine

    init {
        val factory = KotlinJsr223JvmLocalScriptEngineFactory()
        engine = factory.scriptEngine as org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngine
        val bindings: Bindings? = engine.getBindings(ScriptContext.GLOBAL_SCOPE)
        defs.entrySet().forEach { (key, value) ->
            engine.put(key, value)
            bindings?.put(key, value)
        }
    }

    override fun run(code: String): Any? {
        return wrapExecution {
            val wrapCode = wrapCode(code)
            println("""Running:
                |   ${wrapCode.trimIndent().replace("\n", "\n\t")}
                |""".trimMargin())
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
            out.add("val $key : $typeStr = $fqClassName.retrievalIndex.get(\"$uuid\")?.get()!! as $typeStr\n")
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
        val storageMap = java.util.WeakHashMap<Object, UUID>()
        val retrievalIndex = java.util.HashMap<UUID, WeakReference<Object>>()
    }

    override fun getLanguage(): String {
        return "Kotlin"
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