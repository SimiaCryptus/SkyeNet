package com.simiacryptus.skyenet

import com.simiacryptus.skyenet.heart.JSR223Interpreter


/**
 * The heart is the interface to the GraalVM JavaScript engine for the SkyeNet system
 * It powers the body.
 */
class Heart(
    prefix: String = "",
    defs: Map<String, Any> = mapOf(),
) : JSR223Interpreter(prefix, defs)