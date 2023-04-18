package com.simiacryptus.skyenet.heart

import com.simiacryptus.skyenet.Heart
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.URI
import java.util.*
import javax.tools.*
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
open class JavaInterpreter(val defs: Map<String, Any> = HashMap()) : Heart {
    private val guidMap = HashMap<String, Any>()

    init {
        defs.entries.forEach { t ->
            val key = t.key
            val value = t.value
            val guid = UUID.randomUUID().toString()
            guidMap.put(guid, value)
            System.setProperty("java.def.$key", guid)
        }
    }

    private val compiler: JavaCompiler = ToolProvider.getSystemJavaCompiler()
    override fun getLanguage(): String {
        return "Java"
    }

    override fun run(code: String): Any? {
        val (className, wrappedCode) = wrapJavaCode(code)
        val classLoader = InMemoryClassLoader()
        val (diagnosticCollector, success) = compile(className, wrappedCode, classLoader)
        return wrapExecution {
            if (success) {
                val clazz = classLoader.loadClass(className)
                val method = clazz.getMethod("runSnippet")
                try {
                    method.invoke(null)
                } catch (e: Exception) {
                    throw e.cause ?: e
                }
            } else {
                val errors = diagnosticCollector.diagnostics.joinToString("\n")
                Exception("Compilation errors:\n$errors")
            }
        }
    }

    data class Pair<A, B>(val a: A, val b: B)

    private fun compile(
        className: String,
        wrappedCode: String,
        classLoader: InMemoryClassLoader,
    ): Pair<DiagnosticCollector<JavaFileObject>, Boolean> {
        val compilationUnits = listOf(stringJavaFileObject(className, wrappedCode))
        val diagnosticCollector = DiagnosticCollector<JavaFileObject>()
        val fileManager = InMemoryJavaFileManager(compiler, classLoader)
        val success = compiler.getTask(null, fileManager, diagnosticCollector, null, null, compilationUnits).call()
        return Pair(diagnosticCollector, success)
    }
    override fun validate(code: String): Exception? {
        val (className, wrappedCode) = wrapJavaCode(code)
        val classLoader = InMemoryClassLoader()
        val (diagnosticCollector, success) = compile(className, wrappedCode, classLoader)
        return if (success) {
            null
        } else {
            val errors = diagnosticCollector.diagnostics.joinToString("\n") { it.toString() }
            Exception("Compilation errors:\n$errors")
        }
    }


    private fun wrapJavaCode(code: String): Pair<String, String> {
        // Class name with random hex
        val className = "JavaSnippet" + UUID.randomUUID().toString().replace("-", "")
        val definitions = defs.entries.joinToString("\n") { (key, _) ->
            val guid = System.getProperty("java.def.$key")
            "public static Object $key = guidMap.get(\"$guid\");"
        }

        // Split code into import section and code section
        val (imports, codeSection) = code.split("\n").partition { it.startsWith("import ") }

        val wrappedCode = """
            |import java.util.Map;
            |import java.util.UUID;
            |${imports.joinToString("\n|")}
            |
            |public class $className {
            |    private static final Map<String, Object> guidMap = initGuidMap();
            |
            |    $definitions
            |
            |    private static Map<String, Object> initGuidMap() {
            |        Map<String, Object> map = new java.util.HashMap<>();
            |        ${guidMap.entries.joinToString("\n|        ") { (guid, _) -> "map.put(\"$guid\", ${className}.class.getClassLoader().loadClass(\"JavaInterpreter\").getMethod(\"getGuidMap\").invoke(null).get(\"$guid\"));" }}
            |        return map;
            |    }
            |
            |    public static Object runSnippet() {
            |        ${codeSection.joinToString("\n|        ")}
            |        return null;
            |    }
            |}
        """.trimMargin()
        return Pair(className, wrappedCode)
    }

    class stringJavaFileObject(private val className: String, private val code: String) : SimpleJavaFileObject(
        URI.create(
            "string:///" + className.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension
        ), JavaFileObject.Kind.SOURCE
    ) {
        override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence = code
    }

    class InMemoryClassLoader : ClassLoader() {
        private val classes = mutableMapOf<String, ByteArray>()

        fun addClass(name: String, bytes: ByteArray) {
            classes[name] = bytes
        }

        override fun findClass(name: String): Class<*> {
            val bytes = classes[name] ?: throw ClassNotFoundException(name)
            return defineClass(name, bytes, 0, bytes.size)
        }
    }

    class InMemoryJavaFileManager(private val compiler: JavaCompiler, private val classLoader: InMemoryClassLoader) :
        javax.tools.ForwardingJavaFileManager<JavaFileManager>(compiler.getStandardFileManager(null, null, null)) {
        override fun getJavaFileForOutput(
            location: JavaFileManager.Location?,
            className: String,
            kind: JavaFileObject.Kind,
            sibling: FileObject?,
        ): JavaFileObject {
            return object : SimpleJavaFileObject(
                URI.create(
                    "string:///" + className.replace(
                        '.',
                        '/'
                    ) + kind.extension
                ), kind
            ) {
                override fun openOutputStream(): OutputStream {
                    return ByteArrayOutputStream().also { byteArrayOutputStream ->
                        classLoader.addClass(className, byteArrayOutputStream.toByteArray())
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun getGuidMap(): MutableMap<String, Any> {
            return (Thread.currentThread().contextClassLoader.loadClass("JavaInterpreter").getDeclaredConstructor()
                .newInstance() as JavaInterpreter).guidMap
        }
    }

}