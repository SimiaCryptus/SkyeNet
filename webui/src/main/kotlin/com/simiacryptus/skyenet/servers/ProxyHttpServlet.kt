package com.simiacryptus.skyenet.servers

import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.ContextHandlerCollection
import org.eclipse.jetty.servlet.ServletHandler
import org.eclipse.jetty.servlet.ServletHolder
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/**
 * A simple reverse proxy that supports the OpenAI API
 */
class ProxyHttpServlet(
    private val targetUrl : String = "https://api.openai.com/v1"
) : HttpServlet() {

    override fun service(req: HttpServletRequest, resp: HttpServletResponse) {
        val path = req.pathInfo
        val url = URI(targetUrl).resolve(path).toString()
        println("Proxying $path to $url")
        val proxyConnection = URL(url).openConnection() as HttpURLConnection
        proxyConnection.requestMethod = req.method
        proxyConnection.doOutput = true
        proxyConnection.doInput = true
        req.headerNames.iterator().forEach { name ->
            req.getHeaders(name).iterator().forEach { value ->
                proxyConnection.setRequestProperty(name, value)
            }
        }
        proxyConnection.connect()
        req.inputStream.copyTo(proxyConnection.outputStream)
        proxyConnection.inputStream.copyTo(resp.outputStream)
        proxyConnection.disconnect()
    }

    companion object {
        // main
        @JvmStatic
        fun main(args: Array<String>) {
            test()
        }

        fun test() {
            // Start a jetty server, and add 2 servlets: the proxy, and a test servlet
            val server = Server(8080)
            val contextHandlerCollection = ContextHandlerCollection()
            val servletHandler = ServletHandler()
            servletHandler.server = server
            servletHandler.addServletWithMapping(ServletHolder(ProxyHttpServlet("http://localhost:8080")), "/proxy/*")
            servletHandler.addServletWithMapping(ServletHolder(object : HttpServlet() {
                override fun service(req: HttpServletRequest, resp: HttpServletResponse) {
                    resp.writer.println("Hello, world!")
                }
            }), "/test")
            contextHandlerCollection.addHandler(servletHandler)
            server.handler = contextHandlerCollection
            server.start()
            // Test the proxy
            val connection = URL("http://localhost:8080/proxy/test").openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.doOutput = true
            connection.doInput = true
            connection.connect()
            val inputStream = connection.inputStream
            val outputStream = System.out
            inputStream.copyTo(outputStream)
            connection.disconnect()
            server.stop()
        }
    }


}