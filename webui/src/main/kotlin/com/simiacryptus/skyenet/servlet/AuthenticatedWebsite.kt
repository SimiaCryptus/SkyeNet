package com.simiacryptus.skyenet.servlet

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.oauth2.Oauth2
import com.google.api.services.oauth2.model.Userinfo
import com.simiacryptus.skyenet.ApplicationBase.Companion.getCookie
import com.simiacryptus.skyenet.config.ApplicationServices
import com.simiacryptus.skyenet.config.AuthenticationManager
import com.simiacryptus.skyenet.config.AuthenticationManager.Companion.COOKIE_NAME
import jakarta.servlet.*
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.servlet.FilterHolder
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.webapp.WebAppContext
import java.io.InputStream
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*



open class AuthenticatedWebsite(
    val redirectUri: String,
    val applicationName: String,
    key: () -> InputStream?
) {

    open fun newUserSession(userInfo: Userinfo, sessionId: String) {
        log.info("User $userInfo logged in with session $sessionId")
        ApplicationServices.authenticationManager.setUser(sessionId, AuthenticationManager.UserInfo(
            id = userInfo.id,
            email = userInfo.email,
            name = userInfo.name,
            picture = userInfo.picture
        ))
    }

    open fun configure(context: WebAppContext, addFilter: Boolean = true): WebAppContext {
        context.addServlet(ServletHolder("googleLogin", GoogleLoginServlet()), "/googleLogin")
        context.addServlet(ServletHolder("oauth2callback", OAuth2CallbackServlet()), "/oauth2callback")
        if (addFilter) context.addFilter(FilterHolder(SessionIdFilter()), "/*", EnumSet.of(DispatcherType.REQUEST))
        return context
    }

    open fun isSecure(request: HttpServletRequest) =
        setOf("/googleLogin", "/oauth2callback").none { request.requestURI.startsWith(it) }

    private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val clientSecrets: GoogleClientSecrets = GoogleClientSecrets.load(
        jsonFactory,
        InputStreamReader(key()!!)
    )

    private val flow = GoogleAuthorizationCodeFlow.Builder(
        httpTransport,
        jsonFactory,
        clientSecrets,
        listOf(
            "https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/userinfo.profile"
        )
    ).build()

    private inner class GoogleLoginServlet : HttpServlet() {
        override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
            val redirect = req.getParameter("redirect") ?: ""
            val state = URLEncoder.encode(redirect, StandardCharsets.UTF_8.toString())
            val authorizationUrl = // don't want to specify redirectUri to give control of it to user of this class
                GoogleAuthorizationCodeRequestUrl(
                    /* authorizationServerEncodedUrl = */ flow.authorizationServerEncodedUrl,
                    /* clientId = */ flow.clientId,
                    /* redirectUri = */ redirectUri,
                    /* scopes = */ flow.scopes
                )
                    .setAccessType(flow.accessType)
                    .setApprovalPrompt(flow.approvalPrompt)
                    .setState(state)
                    .build()
            resp.sendRedirect(authorizationUrl)
        }
    }

    private inner class SessionIdFilter : Filter {

        override fun init(filterConfig: FilterConfig?) {}

        override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
            if (request is HttpServletRequest && response is HttpServletResponse) {
                if (isSecure(request)) {
                    val sessionIdCookie = request.getCookie()
                    if (sessionIdCookie == null || !ApplicationServices.authenticationManager.containsKey(sessionIdCookie)) {
                        response.sendRedirect("/googleLogin")
                        return
                    }
                }
            }
            chain.doFilter(request, response)
        }

        override fun destroy() {}
    }

    private inner class OAuth2CallbackServlet : HttpServlet() {
        override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
            val code = req.getParameter("code")
            if (code != null) {
                val tokenResponse = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute()
                val credential = flow.createAndStoreCredential(tokenResponse, null)
                val oauth2 =
                    Oauth2.Builder(httpTransport, jsonFactory, credential).setApplicationName(applicationName).build()
                val userInfo: Userinfo = oauth2.userinfo().get().execute()
                val sessionID = UUID.randomUUID().toString()
               newUserSession(userInfo, sessionID)
                val sessionCookie = Cookie(COOKIE_NAME, sessionID)
                sessionCookie.path = "/"
                sessionCookie.isHttpOnly = false
                resp.addCookie(sessionCookie)
                val redirect = req.getParameter("state")?.urlDecode()
                resp.sendRedirect(redirect ?: "/")
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Authorization code not found")
            }
        }
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(AuthenticatedWebsite::class.java)

        fun String.urlDecode(): String? = try {
            URLDecoder.decode(this, StandardCharsets.UTF_8.toString())
        } catch (e: UnsupportedEncodingException) {
            this
        }

    }

}

