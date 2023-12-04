package com.simiacryptus.skyenet.webui.servlet

import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.webui.application.ApplicationServer.Companion.getCookie
import jakarta.servlet.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class SessionIdFilter(
    val isSecure: (HttpServletRequest) -> Boolean,
    val loginRedirect: String
) : Filter {

    override fun init(filterConfig: FilterConfig?) {}

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        if (request is HttpServletRequest && response is HttpServletResponse) {
            if (isSecure(request)) {
                val sessionIdCookie = request.getCookie()
                if (sessionIdCookie == null || null == ApplicationServices.authenticationManager.getUser(
                        sessionIdCookie
                    )
                ) {
                    response.sendRedirect(loginRedirect)
                    return
                }
            }
        }
        chain.doFilter(request, response)
    }

    override fun destroy() {}
}