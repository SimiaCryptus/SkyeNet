package com.simiacryptus.skyenet

interface Heart {

    fun getLanguage(): String
    fun run(code: String): Any?
    fun validate(code: String): Exception?

    fun wrapCode(code: String): String = code
    fun <T : Any> wrapExecution(fn: java.util.function.Supplier<T?>): T? = fn.get()

    companion object {
        @Suppress("unused")
        private class TestObject {
            fun square(x: Int): Int = x * x
        }
        private interface TestInterface {
            fun square(x: Int): Int
        }
        @JvmStatic
        fun test(factory: java.util.function.Function<Map<String, Any>, Heart>) {
            val testImpl = object : TestInterface {
                override fun square(x: Int): Int = x * x
            }
            with(factory.apply(mapOf("message" to "hello"))) {
                test("hello", run("message"))
            }
            with(factory.apply(mapOf("function" to TestObject()))) {
                test(25, run("function.square(5)"))
            }
            with(factory.apply(mapOf("function" to testImpl))) {
                test(25, run("function.square(5)"))
            }
        }

        private fun <T : Any> test(expected: T, actual: T?) {
            require(expected == actual) { actual.toString() }
        }
    }
}