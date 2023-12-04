package com.simiacryptus.skyenet.kotlin

import java.net.URL
import java.util.*

class CompositeClassLoader(private val delegates: List<ClassLoader>) : ClassLoader() {

    override fun findClass(name: String): Class<*> {
        for (delegate in delegates) {
            try {
                return delegate.loadClass(name)
            } catch (e: ClassNotFoundException) {
                // Ignore and try the next class loader
            }
        }
        throw ClassNotFoundException("Class not found in any delegate class loader: $name")
    }

    override fun getResource(name: String): URL? {
        for (delegate in delegates) {
            val resource = delegate.getResource(name)
            if (resource != null) {
                return resource
            }
        }
        return null
    }

    override fun getResources(name: String): Enumeration<URL> {
        val allResources = mutableListOf<URL>()
        for (delegate in delegates) {
            val resources = delegate.getResources(name)
            while (resources.hasMoreElements()) {
                allResources.add(resources.nextElement())
            }
        }
        return Collections.enumeration(allResources)
    }
}