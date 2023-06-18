package com.simiacryptus.skyenet.heart

import com.simiacryptus.skyenet.HeartTestBase
import java.util.zip.ZipFile

class KotlinLocalInterpreterTest : HeartTestBase() {

//    init {
//        val classpath = getClasspath()
//        println("Classpath: \n" + classpath.joinToString("\n") { "\t" + it })
//        searchClasspath("Serializable", classpath)
//    }

    override fun newInterpreter(map: java.util.Map<String, Object>) = KotlinLocalInterpreter(map)

    companion object {
        fun getClasspath() =
            System.getProperty("java.class.path").split(System.getProperty("path.separator")).toList().sorted()

        fun searchClasspath(name: String, classpath: List<String> = getClasspath()) {
            val jars = classpath.filter { it.endsWith(".jar") }.map { ZipFile(it) }
            jars.filter { !it.entries().toList().filter { it.name.contains(name) }.isEmpty() }.forEach {
                println("Found $name: ${it.name}")
                it.entries().toList().filter { it.name.contains(name) }.forEach(::println)
            }
        }
    }
}