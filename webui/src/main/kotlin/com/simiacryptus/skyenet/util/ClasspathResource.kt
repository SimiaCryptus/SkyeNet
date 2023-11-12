package com.simiacryptus.skyenet.util

import org.eclipse.jetty.util.URIUtil
import org.eclipse.jetty.util.resource.Resource
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.nio.channels.ReadableByteChannel

class ClasspathResource(
    val url: URL,
    @Transient private var useCaches: Boolean = __defaultUseCaches,
) : Resource() {
    private val classLoader: ClassLoader = this@ClasspathResource.javaClass.classLoader
    val path = url.path.split("jar!/").last()

    override fun close() {
    }

    override fun exists(): Boolean {
        val resource = classLoader.getResourceAsStream(path)
        if (resource == null) {
            LOG.info("Resource not found: {}", path)
            return false
        }
        resource.close()
        return true
    }

    override fun isDirectory(): Boolean {
        return exists() && url.toExternalForm().endsWith("/")
    }

    override fun lastModified(): Long {
        return -1
    }

    override fun length(): Long {
        return inputStream?.readAllBytes()?.size?.toLong() ?: -1L
    }

    override fun getURI(): URI {
        return try {
            url.toURI()
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
    }

    @Throws(IOException::class)
    override fun getFile(): File? {
        return null
    }

    override fun getName(): String {
        return url.toExternalForm()
    }

    @Throws(IOException::class)
    override fun getInputStream(): InputStream? {
        return classLoader.getResourceAsStream(path)
    }

    @Throws(IOException::class)
    override fun getReadableByteChannel(): ReadableByteChannel? {
        return null
    }

    @Throws(SecurityException::class)
    override fun delete(): Boolean {
        throw SecurityException("Delete not supported")
    }

    @Throws(SecurityException::class)
    override fun renameTo(dest: Resource): Boolean {
        throw SecurityException("RenameTo not supported")
    }

    override fun list(): Array<String>? {
        return null
    }

    @Throws(IOException::class)
    override fun addPath(path: String): Resource {
        if (URIUtil.canonicalPath(path) == null)
            throw MalformedURLException(path)
        val newUrl = URL(URIUtil.addEncodedPaths(url.toExternalForm(), URIUtil.encodePath(path)))
        return ClasspathResource(newUrl, useCaches = useCaches)
    }

    override fun toString(): String {
        return url.toExternalForm()
    }

    override fun hashCode(): Int {
        return url.toExternalForm().hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is ClasspathResource && url.toExternalForm() == other.url.toExternalForm()
    }

    @Throws(MalformedURLException::class)
    override fun isContainedIn(containingResource: Resource): Boolean {
        return false
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ClasspathResource::class.java)
    }
}