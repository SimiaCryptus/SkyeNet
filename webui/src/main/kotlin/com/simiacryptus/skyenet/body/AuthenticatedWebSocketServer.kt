package com.simiacryptus.skyenet.body
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

abstract class AuthenticatedWebsite {

    abstract val redirectUri: String
    abstract val applicationName: String
    abstract fun getKey(): InputStream?
    open fun newUserSession(userInfo: Userinfo, sessionId: String) {
        logger.info("User $userInfo logged in with session $sessionId")
    }

    private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val clientSecrets: GoogleClientSecrets = GoogleClientSecrets.load(
        jsonFactory,
        InputStreamReader(getKey())
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

    open fun configure(webAppContext: WebAppContext) {
        val googleLoginServletHolder = ServletHolder("googleLogin", GoogleLoginServlet())
        webAppContext.addServlet(googleLoginServletHolder, "/googleLogin")
        val oauth2CallbackServletHolder = ServletHolder("oauth2callback", OAuth2CallbackServlet())
        webAppContext.addServlet(oauth2CallbackServletHolder, "/oauth2callback")
        webAppContext.addFilter(FilterHolder(SessionIdFilter()), "/*", EnumSet.of(DispatcherType.REQUEST))
    }

    inner class SessionIdFilter : Filter {
        override fun init(filterConfig: FilterConfig?) {}
        override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
            if (request is HttpServletRequest && response is HttpServletResponse) {
                val path = request.requestURI
                if (setOf("/googleLogin", "/oauth2callback").none { path.startsWith(it) }) {
                    val sessionIdCookie = request.cookies?.firstOrNull { it.name == "sessionId" }
                    if (sessionIdCookie == null || !users.containsKey(sessionIdCookie.value)) {
                        response.sendRedirect("/googleLogin")
                        return
                    }
                }
            }

            chain.doFilter(request, response)
        }

        override fun destroy() {}
    }

    val users = HashMap<String, Userinfo>()

    inner class OAuth2CallbackServlet : HttpServlet() {
        override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
            val code = req.getParameter("code")
            if (code != null) {
                val tokenResponse = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute()
                val credential = flow.createAndStoreCredential(tokenResponse, null)
                val oauth2 = Oauth2.Builder(httpTransport, jsonFactory, credential).setApplicationName(applicationName).build()
                val userInfo: Userinfo = oauth2.userinfo().get().execute()
                val sessionID = UUID.randomUUID().toString()
                users[sessionID] = userInfo
                newUserSession(userInfo, sessionID)
                val sessionCookie = Cookie("sessionId", sessionID)
                sessionCookie.path = "/"
                sessionCookie.isHttpOnly = true
                resp.addCookie(sessionCookie)
                resp.sendRedirect("/")
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Authorization code not found")
            }
        }
    }

    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(AuthenticatedWebsite::class.java)
    }

}