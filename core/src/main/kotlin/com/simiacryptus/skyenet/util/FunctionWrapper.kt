@file:Suppress("unused")

package com.simiacryptus.skyenet.util

import com.simiacryptus.jopenai.util.JsonUtil
import java.io.Closeable
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class FunctionWrapper(val inner: FunctionInterceptor) : FunctionInterceptor {
    inline fun <reified T:Any> wrap(crossinline fn: () -> T) = inner.intercept(T::class.java) { fn() }
    inline fun <P:Any, reified T:Any> wrap(p: P, crossinline fn: (P) -> T) = inner.intercept(p, T::class.java) { fn(it) }
    inline fun <P1:Any, P2:Any, reified T:Any> wrap(p1: P1, p2: P2, crossinline fn: (P1, P2) -> T) =
        inner.intercept(p1, p2, T::class.java) { p1, p2 -> fn(p1, p2) }

    override fun <T : Any> intercept(returnClazz: Class<T>, fn: () -> T) = inner.intercept(returnClazz, fn)

    override fun <P : Any, T : Any> intercept(params: P, returnClazz: Class<T>, fn: (P) -> T) = inner.intercept(params, returnClazz, fn)

    override fun <P1 : Any, P2 : Any, T : Any> intercept(
        p1: P1,
        p2: P2,
        returnClazz: Class<T>,
        fn: (P1, P2) -> T
    ) = inner.intercept(p1, p2, returnClazz, fn)
}

interface FunctionInterceptor {
    fun <T:Any> intercept(returnClazz: Class<T>, fn: () -> T) = fn()
    fun <P:Any, T:Any> intercept(params: P, returnClazz: Class<T>, fn: (P) -> T) = fn(params)
    fun <P1:Any, P2:Any, T:Any> intercept(p1: P1, p2: P2, returnClazz: Class<T>, fn: (P1, P2) -> T) =
        intercept(listOf(p1, p2), returnClazz) {
            @Suppress("UNCHECKED_CAST")
            fn(it[0] as P1, it[1] as P2)
        }
}

class NoopFunctionInterceptor : FunctionInterceptor {
    override fun <T:Any> intercept(returnClazz: Class<T>, fn: () -> T) = fn()
    override fun <P:Any, T:Any> intercept(params: P, returnClazz: Class<T>, fn: (P) -> T) = fn(params)
}

class JsonFunctionRecorder(baseDir: File) : FunctionInterceptor, Closeable {
    private val baseDirectory = baseDir.apply {
        if(exists()) {
            throw IllegalStateException("File already exists: $this")
        }
        mkdirs()
    }
    private val sequenceId = AtomicInteger(0)

    override fun close() {
        // No resources to close in this implementation
    }

    override fun <T : Any> intercept(returnClazz: Class<T>, fn: () -> T): T {
        val dir = operationDir()
        val result = fn()
        File(dir, "output.json").writeText(JsonUtil.toJson(result))
        return result
    }

    override fun <P : Any, T : Any> intercept(params: P, returnClazz: Class<T>, fn: (P) -> T): T {
        val dir = operationDir()
        File(dir, "input.json").writeText(JsonUtil.toJson(params))
        val result = fn(params)
        File(dir, "output.json").writeText(JsonUtil.toJson(result))
        return result
    }

    private fun operationDir(): File {
        val id = sequenceId.incrementAndGet().toString().padStart(3, '0')
        val yyyyMMddHHmmss = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(java.time.LocalDateTime.now())
        val internalClassList = listOf(
            java.lang.Thread::class.java,
            JsonFunctionRecorder::class.java,
            FunctionWrapper::class.java,
            FunctionInterceptor::class.java,
            NoopFunctionInterceptor::class.java,
        )
        // Get the caller method name from the stack trace (first caller not in internalClassList)
        val caller = Thread.currentThread().stackTrace
            .filter { !internalClassList.contains(Class.forName(it.className)) }
            .filter { it.methodName != "intercept" }
            .firstOrNull()
        val methodName = caller?.methodName ?: "unknown"
        val file = File(baseDirectory, "$id-$yyyyMMddHHmmss-$methodName")
        if(file.exists()) {
            throw IllegalStateException("File already exists: $file")
        }
        file.mkdirs()
        return file
    }
}