@file:Suppress("unused")

package com.simiacryptus.skyenet.util

import com.simiacryptus.util.JsonUtil
import java.io.Closeable
import java.io.File

class FunctionWrapper(val inner: FunctionInterceptor) : FunctionInterceptor {
    inline fun <reified T:Any> wrap(crossinline fn: () -> T) = inner.intercept(T::class.java) { fn() }
    inline fun <P:Any, reified T:Any> wrap(p: P, crossinline fn: (P) -> T) = inner.intercept(p, T::class.java) { fn(it) }
    inline fun <P1:Any, P2:Any, reified T:Any> wrap(p1: P1, p2: P2, crossinline fn: (P1, P2) -> T) =
        inner.intercept(p1, p2, T::class.java) { p1, p2 -> fn(p1, p2) }

    override fun <T : Any> intercept(returnClazz: Class<T>, fn: () -> T) = inner.intercept(returnClazz, fn)

    override fun <P : Any, T : Any> intercept(p: P, returnClazz: Class<T>, fn: (P) -> T) = inner.intercept(p, returnClazz, fn)

    override fun <P1 : Any, P2 : Any, T : Any> intercept(
        p1: P1,
        p2: P2,
        returnClazz: Class<T>,
        fn: (P1, P2) -> T
    ) = inner.intercept(p1, p2, returnClazz, fn)
}

interface FunctionInterceptor {
    fun <T:Any> intercept(returnClazz: Class<T>, fn: () -> T) = fn()
    fun <P:Any, T:Any> intercept(p: P, returnClazz: Class<T>, fn: (P) -> T) = fn(p)
    fun <P1:Any, P2:Any, T:Any> intercept(p1: P1, p2: P2, returnClazz: Class<T>, fn: (P1, P2) -> T) =
        intercept(listOf(p1, p2), returnClazz) {
            @Suppress("UNCHECKED_CAST")
            fn(it[0] as P1, it[1] as P2)
        }
}

class NoopFunctionInterceptor : FunctionInterceptor {
    override fun <T:Any> intercept(returnClazz: Class<T>, fn: () -> T) = fn()
    override fun <P:Any, T:Any> intercept(p: P, returnClazz: Class<T>, fn: (P) -> T) = fn(p)
}

class JsonFunctionRecorder(file: File) : FunctionInterceptor, Closeable {
    private val fileOutput = file.outputStream().bufferedWriter()

    override fun close() {
        fileOutput.close()
    }

    override fun <T:Any> intercept(returnClazz: Class<T>, fn: () -> T): T {
        val result = fn()
        synchronized(fileOutput) {
            fileOutput.append(JsonUtil.toJson(result))
            fileOutput.flush()
        }
        return result
    }

    override fun <P:Any, T:Any> intercept(p: P, returnClazz: Class<T>, fn: (P) -> T): T {
        synchronized(fileOutput) {
            fileOutput.append(JsonUtil.toJson(p))
            fileOutput.append("\n")
            fileOutput.flush()
        }
        val result = fn(p)
        synchronized(fileOutput) {
            fileOutput.append(JsonUtil.toJson(result))
            fileOutput.flush()
        }
        return result
    }
}
