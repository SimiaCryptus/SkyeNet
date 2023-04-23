@file:Suppress("MemberVisibilityCanBePrivate")

package com.simiacryptus.skyenet

import com.simiacryptus.openai.OpenAIClient
import java.util.Collections.emptyMap
import java.util.concurrent.atomic.AtomicReference


/**
 * The body is the physical manifestation of the brain.
 * It connects the heart and the brain, providing a framework for action
 */
open class Body(
    val api: OpenAIClient,
    val apiObjects: Map<String, Any> = emptyMap(),
    val heart: Heart,
    val brain: Brain = Brain(
        api = api,
        apiObjects = apiObjects,
        language = heart.getLanguage(),
    ),
) {

    open fun run(
        describedInstruction: String,
        codedInstruction: String = brain.implement(describedInstruction),
        retries: Int = 3,
    ): Any? {
        try {
            return heart.run(codedInstruction)
        } catch (e: Exception) {
            if (retries <= 0) throw e
            val fixCommand = brain.fixCommand(describedInstruction, codedInstruction, e)
            return run(describedInstruction, fixCommand, retries - 1)
        }
    }

    open fun validate(
        describedInstruction: String,
        codedInstruction: String = brain.implement(describedInstruction),
        retries: Int = 3,
    ): String {
        val exception = heart.validate(codedInstruction)
        if (null != exception) {
            if (retries <= 0) throw exception
            if (null == exception.message || exception.message.toString().trim().isEmpty()) throw exception
            log.info("Error: ${exception.message}")
            val fixCommand = brain.fixCommand(describedInstruction, codedInstruction, exception)
            return validate(describedInstruction, fixCommand, retries - 1)
        } else {
            return codedInstruction
        }
    }

    companion object {
        val log = org.slf4j.LoggerFactory.getLogger(Body::class.java)!!
    }

}


