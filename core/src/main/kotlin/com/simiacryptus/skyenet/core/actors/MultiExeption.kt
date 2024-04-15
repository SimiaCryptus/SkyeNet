package com.simiacryptus.skyenet.core.actors

import com.simiacryptus.skyenet.core.actors.CodingActor.Companion.indent

class MultiExeption(exceptions: Collection<Throwable>) : RuntimeException(
    exceptions.joinToString("\n\n") { "```text\n${/*escapeHtml4*/(it.stackTraceToString())/*.indent("  ")*/}\n```" }
)
