@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

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
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersion
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.ref.WeakReference
import java.lang.reflect.Proxy
import java.util.*
import java.util.Map
import javax.script.Bindings
import javax.script.ScriptContext
import kotlin.script.experimental.api.dependencies
import kotlin.script.experimental.api.with
import kotlin.script.experimental.host.ScriptDefinition
import kotlin.script.experimental.jsr223.KOTLIN_JSR223_RESOLVE_FROM_CLASSLOADER_PROPERTY
import kotlin.script.experimental.jsr223.KotlinJsr223DefaultScript
import kotlin.script.experimental.jvm.JvmDependencyFromClassLoader
import kotlin.script.experimental.jvm.JvmScriptCompilationConfigurationBuilder
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.scriptCompilationClasspathFromContext
import kotlin.script.experimental.jvmhost.createJvmScriptDefinitionFromTemplate
import kotlin.script.experimental.jvmhost.jsr223.KotlinJsr223ScriptEngineImpl

open class KotlinInterpreter(
    private val defs: Map<String, Object> = HashMap<String, Object>() as Map<String, Object>
) : Heart {

    override fun validate(code: String): Throwable? {
        val messageCollector = MessageCollectorImpl(code)
        val environment: KotlinCoreEnvironment by lazy {
            KotlinCoreEnvironment.createForProduction(
                {},
                CompilerConfiguration().apply {
                    val arguments = jvmCompilerArguments(code)
                    arguments.configureAnalysisFlags(messageCollector, LanguageVersion.KOTLIN_2_1)
                    arguments.configureLanguageFeatures(messageCollector)
                    put(
                        CommonConfigurationKeys.MODULE_NAME,
                        arguments.moduleName!!
                    )
                    put(
                        CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
                        messageCollector
                    )
                    this.setupJvmSpecificArguments(arguments)
                    this.setupCommonArguments(arguments)
                    this.setupLanguageVersionSettings(arguments)
                },
                EnvironmentConfigFiles.JVM_CONFIG_FILES
            )
        }

        return try {
            val compileBunchOfSources: GenerationState? =
                KotlinToJVMBytecodeCompiler.analyzeAndGenerate(environment)
            //val compileBunchOfSources1 = KotlinToJVMBytecodeCompiler.compileBunchOfSources(environment)
            if (null == compileBunchOfSources) {
                Exception("Compilation failed")
            } else {
                if (messageCollector.errors.isEmpty()) {
                    null
                } else RuntimeException(
                    """
                    |${messageCollector.errors.joinToString("\n") { "Error: $it" }}
                    |${messageCollector.warnings.joinToString("\n") { "Warning: $it" }}
                    """.trimMargin()
                )
            }
        } catch (e: CompilationException) {
            RuntimeException(
                """
                |${e.message}
                |${messageCollector.errors.joinToString("\n") { "Error: " + it }}
                |${messageCollector.warnings.joinToString("\n") { "Warning: " + it }}
                """.trimMargin(), e
            )
        } catch (e: Exception) {
            e
        }
    }

    private class MessageCollectorImpl(
        val code: String,
        val errors: ArrayList<String> = ArrayList(),
        val warnings: ArrayList<String> = ArrayList(),
    ) : MessageCollector {
        override fun clear() {}
        override fun hasErrors() = errors.isNotEmpty()
        override fun report(
            severity: CompilerMessageSeverity,
            message: String,
            location: CompilerMessageSourceLocation?
        ) {
            val msg = errorMessage(code, location?.line ?: 1, location?.column ?: 1, message)
            log.info(msg)
            when {
                severity.isError -> errors.add(msg)
                severity.isWarning -> warnings.add(msg)
            }
        }

    }

    protected open fun jvmCompilerArguments(code: String): K2JVMCompilerArguments {
        val arguments = K2JVMCompilerArguments()
        //arguments.fragmentSources = arrayOf(tempFile.absolutePath)
//        arguments.allowNoSourceFiles = false
        arguments.expression = code
        arguments.classpath = System.getProperty("java.class.path")
//        arguments.compileJava = true
//        arguments.allowAnyScriptsInSourceRoots = true
//        arguments.allowUnstableDependencies = false
//        arguments.checkPhaseConditions = true
        arguments.enableDebugMode = true
//        arguments.enableSignatureClashChecks = true
        arguments.extendedCompilerChecks = true
//        arguments.linkViaSignatures = true
        arguments.reportOutputFiles = true
        arguments.moduleName = "KotlinInterpreter"
        arguments.noOptimize = true
//        arguments.noReflect = true
        arguments.script = true
        arguments.validateIr = true
        arguments.validateBytecode = true
        arguments.verbose = true
//        arguments.javaParameters = true
        arguments.useTypeTable = true
//        arguments.useJavac = true
//        arguments.useFirExtendedCheckers = true
//        arguments.destination = "kotlinBuild"
//        File(arguments.destination).mkdirs()

        return arguments
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

    protected open val scriptEngineFactory by lazy { KotlinScriptEngineFactory() }

    inner class KotlinScriptEngineFactory : KotlinJsr223JvmScriptEngineFactoryBase() {
        override fun getScriptEngine() = KotlinJsr223ScriptEngineImpl(
            this,
            scriptDefinition.compilationConfiguration.with {
                jvm {
                    if (System.getProperty(KOTLIN_JSR223_RESOLVE_FROM_CLASSLOADER_PROPERTY) == "true") {
                        dependencies(JvmDependencyFromClassLoader { Thread.currentThread().contextClassLoader })
                    } else {
                        dependenciesFromCurrentContext()
                    }
                }
            },
            scriptDefinition.evaluationConfiguration
        ) {
            ScriptArgsWithTypes(
                arrayOf(it.getBindings(ScriptContext.ENGINE_SCOPE).orEmpty()),
                arrayOf(Bindings::class)
            )
        }
    }

    override fun run(code: String): Any? {
        val wrappedCode = wrapCode(code)
        log.info(
            """
            |Running:
            |   ${wrappedCode.trimIndent().replace("\n", "\n\t")}
            |""".trimMargin().trim()
        )
        try {
            return scriptEngineFactory.scriptEngine.eval(wrappedCode)
        } catch (ex: javax.script.ScriptException) {
            var lineNumber = ex.lineNumber
            var column = ex.columnNumber
            if (lineNumber == -1 && column == -1) {
                val match = Regex("\\(.*:(\\d+):(\\d+)\\)").find(ex.message ?: "")
                if (match != null) {
                    lineNumber = match.groupValues[1].toInt()
                    column = match.groupValues[2].toInt()
                }
            }
            throw RuntimeException(
                errorMessage(code, lineNumber, column, ex.message ?: ""), ex
            )
        }
    }

    override fun wrapCode(code: String): String {
        val out = ArrayList<String>()
        val (imports, otherCode) = code.split("\n").partition { it.trim().startsWith("import ") }
        out.addAll(imports)
        defs.entrySet().forEach { (key, value) ->
            val uuid = storageMap.getOrPut(value) { UUID.randomUUID() }
            retrievalIndex.put(uuid, WeakReference(value))
            val fqClassName = KotlinInterpreter::class.java.name.replace("$", ".")
            val typeStr = typeOf(value)
            out.add("val $key : $typeStr = $fqClassName.retrievalIndex.get(java.util.UUID.fromString(\"$uuid\"))?.get()!! as $typeStr\n")
        }
        out.addAll(otherCode)
        return out.joinToString("\n")
    }

    open fun typeOf(value: Any?): String {
        if (value is Proxy) {
            return value.javaClass.interfaces[0].name.replace("$", ".") + "?"
        }
        val replace = value?.javaClass?.name?.replace("$", ".")
        return if (replace != null) ("$replace") else "null"
    }

    companion object {
        val log = LoggerFactory.getLogger(KotlinInterpreter::class.java)
        val storageMap = WeakHashMap<Object, UUID>()
        val retrievalIndex = HashMap<UUID, WeakReference<Object>>()

        fun errorMessage(
            code: String,
            line: Int,
            column: Int,
            message: String
        ) = """
                |$message at line ${line} column ${column}
                |  ${code.split("\n")[line - 1]}
                |  ${" ".repeat(column - 1) + "^"}
                """.trimMargin().trim()
    }

    override fun getLanguage(): String {
        return "Kotlin"
    }

}