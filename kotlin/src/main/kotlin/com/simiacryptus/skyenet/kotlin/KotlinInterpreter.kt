@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.simiacryptus.skyenet.kotlin


import com.simiacryptus.skyenet.core.Interpreter
import com.simiacryptus.skyenet.core.actors.CodingActor
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.incrementalCompilationIsEnabled
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineFactoryBase
import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.*
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.ref.WeakReference
import java.lang.reflect.Proxy
import java.net.URL
import java.net.URLClassLoader
import java.util.*
import kotlin.script.experimental.api.with
import kotlin.script.experimental.jsr223.KotlinJsr223DefaultScriptCompilationConfiguration
import kotlin.script.experimental.jsr223.KotlinJsr223DefaultScriptEvaluationConfiguration
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.scriptCompilationClasspathFromContext
import kotlin.script.experimental.jvmhost.jsr223.KotlinJsr223ScriptEngineImpl

open class KotlinInterpreter(
  private val defs: Map<String, Any> = HashMap<String, Any>(),
) : Interpreter {
  open val classLoader: ClassLoader get() = KotlinInterpreter::class.java.classLoader

  override fun symbols() = defs as kotlin.collections.Map<String, Any>


  override fun validate(code: String): Throwable? {
    val messageCollector = MessageCollectorImpl(code)
    return try {
      val environment = kotlinCoreEnvironment(code, messageCollector)
      val compileBunchOfSources: GenerationState? =
        KotlinToJVMBytecodeCompiler.analyzeAndGenerate(environment)
      if (null == compileBunchOfSources) {
        RuntimeException(
          if (messageCollector.errors.isEmpty()) "Compilation failed"
          else """Compilation failed
          |${messageCollector.errors.joinToString("\n") { "Error: $it" }}
          |${messageCollector.warnings.joinToString("\n") { "Warning: $it" }}
          """.trimMargin()
        )
      } else {
        if (messageCollector.errors.isEmpty()) {
          //compileBunchOfSources.scriptSpecific.resultType
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
    } catch (e: Throwable) {
      e
    }
  }

  open fun kotlinCoreEnvironment(
    code: String,
    messageCollector: MessageCollector
  ): KotlinCoreEnvironment {
    val environment: KotlinCoreEnvironment by lazy {
      KotlinCoreEnvironment.createForProduction(
        {},
        CompilerConfiguration().apply {
          val arguments = K2JVMCompilerArguments()
          arguments.expression = code
          arguments.classpath = System.getProperty("java.class.path")
          arguments.enableDebugMode = true
          arguments.extendedCompilerChecks = true
          arguments.reportOutputFiles = true
          arguments.moduleName = "KotlinInterpreter"
          arguments.noOptimize = true
          arguments.script = true
          arguments.validateIr = true
          arguments.validateBytecode = true
          arguments.verbose = true
          arguments.useTypeTable = true
          arguments.includeRuntime = true
          arguments.noReflect = true
          arguments.useK2 = true
          arguments.jdkHome = System.getProperty("java.home")
          arguments.noInline = true
          arguments.expectActualLinker = true
          arguments.reportOutputFiles = false
          arguments.incrementalCompilation = false
          arguments.allowAnyScriptsInSourceRoots = true
          arguments.ignoreConstOptimizationErrors = true
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
          //setupJvmSpecificArguments(arguments)
          put(JVMConfigurationKeys.INCLUDE_RUNTIME, arguments.includeRuntime)
          put(JVMConfigurationKeys.NO_REFLECT, arguments.noReflect)
          put(JVMConfigurationKeys.JDK_RELEASE, 11)
          put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_11)
          put(JVMConfigurationKeys.JDK_HOME, File(arguments.jdkHome!!))
          put(JVMConfigurationKeys.SAM_CONVERSIONS, JvmClosureGenerationScheme.INDY)
          put(JVMConfigurationKeys.LAMBDAS, JvmClosureGenerationScheme.INDY)
          put(JVMConfigurationKeys.COMPILE_JAVA, false)
          put(JVMConfigurationKeys.USE_JAVAC, false)
          put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, true)
          put(JVMConfigurationKeys.VALIDATE_IR, true)
          put(JVMConfigurationKeys.VALIDATE_BYTECODE, true)

          //setupLanguageVersionSettings(arguments)
          languageVersionSettings = arguments.toLanguageVersionSettings(messageCollector)

          //setupCommonArguments(arguments)
          put(CommonConfigurationKeys.DISABLE_INLINE, arguments.noInline)
          put(CommonConfigurationKeys.USE_FIR_EXTENDED_CHECKERS, arguments.useFirExtendedCheckers)
          put(CommonConfigurationKeys.EXPECT_ACTUAL_LINKER, arguments.expectActualLinker)
          putIfNotNull(CLIConfigurationKeys.INTELLIJ_PLUGIN_ROOT, arguments.intellijPluginRoot)
          put(CommonConfigurationKeys.REPORT_OUTPUT_FILES, arguments.reportOutputFiles)
          put(CommonConfigurationKeys.INCREMENTAL_COMPILATION, incrementalCompilationIsEnabled(arguments))
          put(CommonConfigurationKeys.ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS, arguments.allowAnyScriptsInSourceRoots)
          put(CommonConfigurationKeys.IGNORE_CONST_OPTIMIZATION_ERRORS, arguments.ignoreConstOptimizationErrors)
          val usesK2 = arguments.useK2 || languageVersionSettings.languageVersion.usesK2
          put(CommonConfigurationKeys.USE_FIR, usesK2)
          put(CommonConfigurationKeys.USE_LIGHT_TREE, arguments.useFirLT)
        },
        EnvironmentConfigFiles.JVM_CONFIG_FILES
      )
    }
    //environment.projectEnvironment.configureProjectEnvironment()
    return environment
  }

  class MessageCollectorImpl(
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

  private var lastClassLoader: ClassLoader? = null
  private var lastClassPath: List<File>? = null

  open val scriptEngine: javax.script.ScriptEngine get() = KotlinScriptEngineFactory().scriptEngine

  inner class KotlinScriptEngineFactory : KotlinJsr223JvmScriptEngineFactoryBase() {
    override fun getScriptEngine() = KotlinJsr223ScriptEngineImpl(
      this,
      KotlinJsr223DefaultScriptCompilationConfiguration.with {
        jvm {
          val currentClassLoader = classLoader
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
      },
      KotlinJsr223DefaultScriptEvaluationConfiguration.with {

      }
    ) {
      ScriptArgsWithTypes(
        arrayOf(
//          it.getBindings(ScriptContext.ENGINE_SCOPE).orEmpty()
        ),
        arrayOf(
//          Bindings::class
        )
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
      return scriptEngine.eval(wrappedCode)
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
      throw CodingActor.FailedToImplementException(
        cause = ex,
        message = errorMessage(
          code = wrappedCode,
          line = lineNumber,
          column = column,
          message = ex.message ?: ""
        ),
        language = "Kotlin",
        code = code
      )
    } catch (ex: Throwable) {
      throw CodingActor.FailedToImplementException(cause = ex, language = "Kotlin", code = code)
    }
  }

  override fun wrapCode(code: String): String {
    val out = ArrayList<String>()
    val (imports, otherCode) = code.split("\n").partition { it.trim().startsWith("import ") }
    out.addAll(imports)
    defs.forEach { key, value ->
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
    private val log = LoggerFactory.getLogger(KotlinInterpreter::class.java)
    val storageMap = WeakHashMap<Any, UUID>()
    val retrievalIndex = HashMap<UUID, WeakReference<Any>>()

    fun errorMessage(
      code: String,
      line: Int,
      column: Int,
      message: String
    ) = """
        |```text
        |$message at line ${line} column ${column}
        |  ${if (line < 0) "" else code.split("\n")[line - 1]}
        |  ${if (column < 0) "" else " ".repeat(column - 1) + "^"}
        |```
        """.trimMargin().trim()

    fun ClassLoader.isolatedClassLoader() = URLClassLoader(arrayOf<URL>(), this)
  }

  override fun getLanguage(): String {
    return "Kotlin"
  }

}

