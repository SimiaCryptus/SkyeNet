package com.simiacryptus.skyenet.apps.meta

import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.skyenet.apps.meta.FlowStepDesigner.Companion.fixups
import com.simiacryptus.skyenet.core.actors.CodingActor
import com.simiacryptus.skyenet.interpreter.Interpreter
import kotlin.reflect.KClass

class CodingActorDesigner(
    interpreterClass: KClass<out Interpreter>,
    symbols: Map<String, Any>,
    model: ChatModels,
    temperature: Double
) : CodingActor(
    interpreterClass = interpreterClass,
    symbols = symbols,
    details = """
    |
    |Your task is to design a system that uses gpt "actors" to form a "community" of actors interacting to solve problems.
    |Your task is to implement a "script" or "coding" actor that takes part in a larger system.
    |"Script" actors use a multi-stage process that combines an environment definition of predefined symbols/functions and a pluggable script compilation system using Scala, Kotlin, or Groovy. The actor will return a valid script with a convenient "execute" method. This can provide both simple function calling responses and complex code generation.
    |
    |For context, here is the constructor signature for CodingActor class:
    |```kotlin
    |package com.simiacryptus.skyenet.core.actors
    |
    |import com.simiacryptus.jopenai.models.OpenAIModels
    |import com.simiacryptus.jopenai.describe.AbbrevWhitelistYamlDescriber
    |import com.simiacryptus.jopenai.describe.TypeDescriber
    |import com.simiacryptus.skyenet.interpreter.Interpreter
    |import kotlin.reflect.KClass
    |
    |class CodingActor(
    |    val interpreterClass: KClass<out Interpreter>,
    |    val symbols: Map<String, Any> = mapOf(),
    |    val describer: TypeDescriber = AbbrevWhitelistYamlDescriber(
    |        "com.simiacryptus",
    |        "com.github.simiacryptus"
    |    ),
    |    name: String? = interpreterClass.simpleName,
    |    val details: String? = null,
    |    model: OpenAITextModel = OpenAIModels.GPT4o,
    |    val fallbackModel: ChatModels = OpenAIModels.GPT4o,
    |    temperature: Double = 0.1,
    |)
    |```
    |
    |In this code example an example actor is defined with a prompt, name, and a standard configuration:
    |```kotlin
    |import com.simiacryptus.skyenet.core.actors.CodingActor
    |import com.simiacryptus.skyenet.kotlin.KotlinInterpreter
    |
    |fun exampleCodingActor() = CodingActor(
    |    interpreterClass = KotlinInterpreter::class,
    |    details = ""${'"'}
    |    |You are a software implementation assistant.
    |    |
    |    |Defined functions:
    |    |* ...
    |    |
    |    |Expected code structure:
    |    |* ...
    |    ""${'"'}.trimMargin().trim(),
    |)
    |```
    |
    |Respond to the request with an instantiation function of the requested actor, similar to the provided example.
    |DO NOT subclass the CodingActor class. Use the constructor directly within the function.
    |
    """.trimMargin().trim(),
    model = model,
    temperature = temperature,
) {
    init {
        evalFormat = false
        codeInterceptor = { fixups(it) }
    }
}