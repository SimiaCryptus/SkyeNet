package com.simiacryptus.skyenet.webui

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.oauth2.Oauth2
import com.google.api.services.oauth2.model.Userinfo
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
import java.util.*

open class AuthenticatedWebsite(
    val redirectUri: String,
    val applicationName: String,
    private val key: ()->InputStream?
) {

    open fun newUserSession(userInfo: Userinfo, sessionId: String) {
        log.info("User $userInfo logged in with session $sessionId")
    }

    private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val clientSecrets: GoogleClientSecrets = GoogleClientSecrets.load(
        jsonFactory,
        InputStreamReader(key())
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


    inner class GoogleLoginServlet : HttpServlet() {
        override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
            val authorizationUrl = flow.newAuthorizationUrl().setRedirectUri(redirectUri).build()
            resp.sendRedirect(authorizationUrl)
        }
    }

    /**
     * Configures the given `WebAppContext` to handle authentication via Google OAuth2.
     * Adds the `GoogleLoginServlet` to handle the login process at `/googleLogin`.
     * Adds the `OAuth2CallbackServlet` to handle the callback from Google at `/oauth2callback`.
     * Adds a `SessionIdFilter` to the context to ensure that session IDs are properly handled.
     *
     * @param context the `WebAppContext` to configure
     */
    open fun configure(context: WebAppContext, addFilter: Boolean = true): WebAppContext {
        context.addServlet(ServletHolder("googleLogin", GoogleLoginServlet()), "/googleLogin")
        context.addServlet(ServletHolder("oauth2callback", OAuth2CallbackServlet()), "/oauth2callback")
        if(addFilter) context.addFilter(FilterHolder(SessionIdFilter()), "/*", EnumSet.of(DispatcherType.REQUEST))
        return context
    }

    inner class SessionIdFilter : Filter {

        override fun init(filterConfig: FilterConfig?) {}

        /**
         * Overrides the doFilter method to intercept incoming requests and responses,
         * check if the request is for the "/googleLogin" or "/oauth2callback" URLs,
         * and redirects to the "/googleLogin" page if the user session is not active.
         *
         * @param request The ServletRequest object that represents the client request
         * @param response The ServletResponse object that represents the client response
         * @param chain The FilterChain object that represents the filter chain
         * @throws IOException if an I/O error occurs
         * @throws ServletException if a servlet-specific error occurs
         */
        override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
            // Check if the request is an HTTP request and the response is an HTTP response
            if (request is HttpServletRequest && response is HttpServletResponse) {
                // Get the path of the request URI
                // Check if the path does not match /googleLogin or /oauth2callback
                if (isSecure(request)) {
                    // Get the sessionId cookie from the request
                    val sessionIdCookie = request.cookies?.firstOrNull { it.name == "sessionId" }
                    // Check if the sessionId is null or is not contained in the users map
                    if (sessionIdCookie == null || !users.containsKey(sessionIdCookie.value)) {
                        // Redirect to the /googleLogin endpoint
                        response.sendRedirect("/googleLogin")
                        return
                    }
                }
            }
            // Call the doFilter method of the next filter in the chain
            chain.doFilter(request, response)
        }

        override fun destroy() {}
    }

    open fun isSecure(request: HttpServletRequest) =
        setOf("/googleLogin", "/oauth2callback").none { request.requestURI.startsWith(it) }

    val users = HashMap<String, Userinfo>()

    inner class OAuth2CallbackServlet : HttpServlet() {
        override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
            val code = req.getParameter("code")
            if (code != null) {
                val tokenResponse = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute()
                val credential = flow.createAndStoreCredential(tokenResponse, null)
                val oauth2 =
                    Oauth2.Builder(httpTransport, jsonFactory, credential).setApplicationName(applicationName).build()
                val userInfo: Userinfo = oauth2.userinfo().get().execute()
                val sessionID = UUID.randomUUID().toString()
                users[sessionID] = userInfo
                newUserSession(userInfo, sessionID)
                val sessionCookie = Cookie("sessionId", sessionID)
                sessionCookie.path = "/"
                sessionCookie.isHttpOnly = false
                resp.addCookie(sessionCookie)
                resp.sendRedirect("/")
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Authorization code not found")
            }
        }
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(AuthenticatedWebsite::class.java)
    }

}