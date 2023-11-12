package com.simiacryptus.skyenet.heart

import com.simiacryptus.skyenet.Heart
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineFactoryBase
import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import org.jetbrains.kotlin.cli.common.setupCommonArguments
import org.jetbrains.kotlin.cli.common.setupLanguageVersionSettings
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.cli.jvm.setupJvmSpecificArguments
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.io.File
import java.lang.ref.WeakReference
import java.util.*
import java.util.Map
import javax.script.Bindings
import javax.script.ScriptContext
import javax.script.ScriptEngine
import kotlin.script.experimental.api.with
import kotlin.script.experimental.jsr223.KOTLIN_JSR223_RESOLVE_FROM_CLASSLOADER_PROPERTY
import kotlin.script.experimental.jsr223.KotlinJsr223DefaultScript
import kotlin.script.experimental.jsr223.KotlinJsr223DefaultScriptEngineFactory
import kotlin.script.experimental.jvm.JvmDependencyFromClassLoader
import kotlin.script.experimental.jvm.JvmScriptCompilationConfigurationBuilder
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.scriptCompilationClasspathFromContext
import kotlin.script.experimental.jvmhost.createJvmScriptDefinitionFromTemplate
import kotlin.script.experimental.jvmhost.jsr223.KotlinJsr223ScriptEngineImpl
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.api.dependencies
import kotlin.script.experimental.host.ScriptDefinition

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
            override fun hasErrors() = errors.isNotEmpty()
            override fun report(
                severity: CompilerMessageSeverity,
                message: String,
                location: CompilerMessageSourceLocation?
            ) {
                val lineText = tempFile.readLines()[location!!.line - 1]
                val carotText = " ".repeat(location.column - 1) + "^"
                val msg = """
                |$message at line ${location.line} column ${location.column}
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
            //addKotlinSourceRoot(tempFile.absolutePath)
//            configureJdkClasspathRoots()
//            System.getProperty("java.class.path").split(File.pathSeparator).forEach {
//                addJvmClasspathRoot(File(it))
//            }
            val k2JVMCompilerArguments = K2JVMCompilerArguments()
            k2JVMCompilerArguments.fragmentSources = arrayOf(tempFile.absolutePath)
            k2JVMCompilerArguments.classpath = System.getProperty("java.class.path")
            k2JVMCompilerArguments.moduleName = "ModuleName"
            k2JVMCompilerArguments.script = true
            this.setupJvmSpecificArguments(k2JVMCompilerArguments)
            this.setupCommonArguments(k2JVMCompilerArguments)
            this.setupLanguageVersionSettings(k2JVMCompilerArguments)
//            put(org.jetbrains.kotlin.config.JVMConfigurationKeys.COMPILE_JAVA, true)
            put(org.jetbrains.kotlin.config.CommonConfigurationKeys.MODULE_NAME, k2JVMCompilerArguments.moduleName!!)
        }

        // Create the compiler environment
        val environment = KotlinCoreEnvironment.createForProduction(
            parentDisposable = {},
            configuration = configuration,
            configFiles = EnvironmentConfigFiles.JVM_CONFIG_FILES
        )

        return try {
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

    private val scriptDefinition: ScriptDefinition = createJvmScriptDefinitionFromTemplate<KotlinJsr223DefaultScript>()
    private var lastClassLoader: ClassLoader? = null
    private var lastClassPath: List<File>? = null

    @Synchronized
    private fun JvmScriptCompilationConfigurationBuilder.dependenciesFromCurrentContext() {
        val currentClassLoader = Thread.currentThread().contextClassLoader
        val classPath = if (lastClassLoader == null || lastClassLoader != currentClassLoader) {
            scriptCompilationClasspathFromContext(
                classLoader = currentClassLoader,
                wholeClasspath = true,
                unpackJarCollections = true
            ).also {
                lastClassLoader = currentClassLoader
                lastClassPath = it
            }
        } else lastClassPath!!
        updateClasspath(classPath)
    }

    val compilationConfiguration = scriptDefinition.compilationConfiguration.with {
        jvm {
            if (System.getProperty(KOTLIN_JSR223_RESOLVE_FROM_CLASSLOADER_PROPERTY) == "true") {
                dependencies(JvmDependencyFromClassLoader { Thread.currentThread().contextClassLoader })
            } else {
                dependenciesFromCurrentContext()
            }
        }
    }

    private val scriptEngineFactory = object : KotlinJsr223JvmScriptEngineFactoryBase() {
        override fun getScriptEngine(): ScriptEngine {
            return KotlinJsr223ScriptEngineImpl(
                this,
                compilationConfiguration,
                scriptDefinition.evaluationConfiguration
            ) { ScriptArgsWithTypes(arrayOf(it.getBindings(ScriptContext.ENGINE_SCOPE).orEmpty()), arrayOf(Bindings::class)) }
        }
    }
    override fun run(code: String): Any? {
        val wrappedCode = wrapCode(code)
        log.info(
            """Running:
        |   ${wrappedCode.trimIndent().replace("\n", "\n\t")}
        |""".trimMargin()
        )

        return scriptEngineFactory.scriptEngine.eval(wrappedCode)
    }

    override fun wrapCode(code: String): String {
        val out = ArrayList<String>()
        val (imports, otherCode) = code.split("\n").partition { it.trim().startsWith("import ") }
        out.addAll(imports)
        //out.add("import kotlin")
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