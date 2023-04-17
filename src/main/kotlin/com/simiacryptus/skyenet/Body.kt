@file:Suppress("MemberVisibilityCanBePrivate")

package com.simiacryptus.skyenet

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.heart.KotlinInterpreter
import java.util.Collections.emptyMap
import java.util.concurrent.atomic.AtomicReference


/**
 * The body is the physical manifestation of the brain.
 * It connects the heart and the brain, providing a framework for action
 */
class Body(
    val api: OpenAIClient,
    val apiObjects: Map<String, Any> = emptyMap(),
    val brain: Brain = Brain(
        api = api,
        apiObjects = apiObjects,
    ),
    val heart: Heart = KotlinInterpreter(apiObjects),
) {
    fun run(
        describedInstruction: String,
        codedInstruction: String = brain.implement(describedInstruction),
        retries: Int = 3,
    ): Any? = execute(
        describedInstruction = describedInstruction,
        codedInstruction = codedInstruction,
        retries = retries
    )

    fun execute(
        describedInstruction: String,
        codedInstruction: String,
        retries: Int = 3,
        finalCode: AtomicReference<String> = AtomicReference(codedInstruction),
    ): Any? {
        try {
            val code = validate(describedInstruction, codedInstruction, retries)
            val run = heart.run(code)
            return run
        } catch (e: Exception) {
            if (retries <= 0) throw e
            val fixCommand = brain.fixCommand(describedInstruction, codedInstruction, e.message ?: "")
            finalCode.set(fixCommand)
            return execute(describedInstruction, fixCommand, retries - 1, finalCode)
        }
    }

    fun validate(
        describedInstruction: String,
        codedInstruction: String = brain.implement(describedInstruction),
        retries: Int = 3,
    ): String {
        val exception = heart.validate(codedInstruction)
        if (null != exception) {
            if (retries <= 0) throw exception
            if (null == exception.message || exception.message.toString().trim().isEmpty()) throw exception
            log.info("Error: ${exception.message}")
            val fixCommand = brain.fixCommand(describedInstruction, codedInstruction, exception.message ?: "")
            return validate(describedInstruction, fixCommand, retries - 1)
        } else {
            return codedInstruction
        }
    }


    companion object {
        val log = org.slf4j.LoggerFactory.getLogger(Body::class.java)!!
    }

}


