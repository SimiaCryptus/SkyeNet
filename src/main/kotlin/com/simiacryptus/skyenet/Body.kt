@file:Suppress("MemberVisibilityCanBePrivate")

package com.simiacryptus.skyenet

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.proxy.Description
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Parameter
import javax.script.ScriptException


/**
 * The body is the physical manifestation of the brain.
 * It connects the heart and the brain, providing a framework for action
 */
class Body(
    val api: OpenAIClient,
    val apiObjects: Map<String, Any> = mapOf(),
    val brain: Brain = Brain(
        api = api,
        apiObjects = apiObjects,
    ),
    val heart: Heart = Heart("", apiObjects)
) {
    fun run(
        describedInstruction: String,
        codedInstruction: String = brain.implement(describedInstruction),
        retries: Int = 3,
    ) {
        println(codedInstruction)
        try {
            val validate = heart.validate(codedInstruction)
            if (null != validate) {
                throw ScriptException(validate)
            }
            heart.run(codedInstruction)
        } catch (e: ScriptException) {
            if (retries <= 0) throw e
            if (e.message.isNullOrBlank()) throw e
            log.info("Error: ${e.message}")
            val fixCommand = brain.fixCommand(describedInstruction, codedInstruction, e.message ?: "")
            run(describedInstruction, fixCommand, retries - 1)
        }
    }


    companion object {
        val log = org.slf4j.LoggerFactory.getLogger(Body::class.java)!!

    }

}


