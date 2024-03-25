package com.simiacryptus.skyenet.core.actors

class MultiExeption(exceptions: Collection<Throwable>) : RuntimeException(
  exceptions.joinToString("\n\n") { "```text\n${it.stackTraceToString()}\n```" }
) {
}
