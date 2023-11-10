package com.simiacryptus.skyenet.heart

import com.simiacryptus.skyenet.Heart
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.io.File
import java.lang.ref.WeakReference
import java.util.*
import java.util.Map
import kotlin.script.experimental.jsr223.KotlinJsr223DefaultScriptEngineFactory


open class KotlinInterpreter(
    private val defs: Map<String, Object> = HashMap<String, Object>() as Map<String, Object>
) : Heart {

    override fun validate(code: String): Throwable? {
        // Create a temporary file for the code
        val tempFile = File.createTempFile("TempKotlin", ".kt").apply {
            writeText(code)
            deleteOnExit()
        }

        // Implement a custom MessageCollector
        val errors = ArrayList<String>()
        val warnings = ArrayList<String>()
        val messageCollector = object : MessageCollector {
            override fun clear() {}
            override fun hasErrors() = false
            override fun report(
                severity: CompilerMessageSeverity,
                message: String,
                location: CompilerMessageSourceLocation?
            ) {
                // Log the message with severity and location
                val line = location!!.line
                val column = location.column
                val lineText = tempFile.readLines()[line - 1]
                val carotText = " ".repeat(column) + "^"
                val msg = """
                |$severity: $message at line ${line} column ${column}
                |  $lineText
                |  $carotText
                """.trimMargin().trim()
                log.info(msg)
                when {
                    severity.isError -> errors.add(msg)
                    severity.isWarning -> warnings.add(msg)
                }
            }
        }

        val configuration = CompilerConfiguration().apply {
            put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
            addKotlinSourceRoot(tempFile.absolutePath)
            configureJdkClasspathRoots()
            System.getProperty("java.class.path").split(File.pathSeparator).forEach {
                addJvmClasspathRoot(File(it))
            }

            put(org.jetbrains.kotlin.config.JVMConfigurationKeys.COMPILE_JAVA, true)
            put(org.jetbrains.kotlin.config.CommonConfigurationKeys.MODULE_NAME, "ModuleName")
        }

        // Create the compiler environment
        val environment = KotlinCoreEnvironment.createForProduction(
            parentDisposable = Disposable {},
            configuration = configuration,
            configFiles = EnvironmentConfigFiles.JVM_CONFIG_FILES
        )

        val bindingContext = KotlinToJVMBytecodeCompiler.analyze(environment)
            ?: throw IllegalStateException("Binding context could not be initialized")
        return try {
            if (bindingContext.isError()) bindingContext.error else {
                val compileBunchOfSources: GenerationState? =
                    KotlinToJVMBytecodeCompiler.analyzeAndGenerate(environment)
                if (null == compileBunchOfSources) {
                    Exception("Compilation failed")
                } else {
                    if (errors.isEmpty()) null
                    else RuntimeException(
                        """
                        |${errors.joinToString("\n") { "Error: " + it }}
                        |${warnings.joinToString("\n") { "Warning: " + it }}
                        """.trimMargin())
                }
            }
        } catch (e: CompilationException) {
            RuntimeException(
                """
                |${e.message}
                |${errors.joinToString("\n") { "Error: " + it }}
                |${warnings.joinToString("\n") { "Warning: " + it }}
                """.trimMargin(), e)
        } catch (e: Exception) {
            e
        }
    }

    override fun run(code: String): Any? {
        val wrappedCode = wrapCode(code)
        log.info(
            """Running:
        |   ${wrappedCode.trimIndent().replace("\n", "\n\t")}
        |""".trimMargin()
        )
        val scriptEngineFactory = KotlinJsr223DefaultScriptEngineFactory()
        return scriptEngineFactory.scriptEngine.eval(wrappedCode)
    }

    override fun wrapCode(code: String): String {
        val out = ArrayList<String>()
        val (imports, otherCode) = code.split("\n").partition { it.trim().startsWith("import ") }
        out.addAll(imports)
        defs.entrySet().forEach { (key, value) ->
            val uuid = storageMap.getOrPut(value) { UUID.randomUUID() }
            retrievalIndex.put(uuid, WeakReference(value))
            val fqClassName = KotlinInterpreter.javaClass.name.replace("$", ".")
            val typeStr = typeOf(value)
            out.add("val $key : $typeStr = $fqClassName.retrievalIndex.get(java.util.UUID.fromString(\"$uuid\"))?.get()!! as $typeStr\n")
        }
        out.addAll(otherCode)
        return out.joinToString("\n")
    }

    open fun typeOf(value: Object?): String {
        if (value is java.lang.reflect.Proxy) {
            return value.javaClass.interfaces[0].name.replace("$", ".") + "?"
        }
        val replace = value?.javaClass?.name?.replace("$", ".")
        return if (replace != null) ("$replace") else "null"
    }

    companion object {
        val log = org.slf4j.LoggerFactory.getLogger(KotlinInterpreter::class.java)
        val storageMap = WeakHashMap<Object, UUID>()
        val retrievalIndex = HashMap<UUID, WeakReference<Object>>()
    }

    override fun getLanguage(): String {
        return "Kotlin"
    }

}