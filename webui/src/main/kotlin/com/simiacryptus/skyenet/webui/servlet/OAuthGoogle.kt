package com.simiacryptus.skyenet.webui.servlet

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.oauth2.Oauth2
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.AuthenticationInterface
import com.simiacryptus.skyenet.core.platform.User
import jakarta.servlet.DispatcherType
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
import java.util.concurrent.TimeUnit

open class OAuthGoogle(
    redirectUri: String,
    val applicationName: String,
    key: () -> InputStream?
) : OAuthBase(redirectUri) {

    override fun configure(context: WebAppContext, addFilter: Boolean): WebAppContext {
        context.addServlet(ServletHolder("googleLogin", LoginServlet()), "/login")
        context.addServlet(ServletHolder("googleLogin", LoginServlet()), "/googleLogin")
        context.addServlet(ServletHolder("oauth2callback", CallbackServlet()), "/oauth2callback")
        if (addFilter) context.addFilter(FilterHolder(SessionIdFilter({ request ->
            setOf("/googleLogin", "/oauth2callback").none { request.requestURI.startsWith(it) }
        }, "/googleLogin")), "/*", EnumSet.of(DispatcherType.REQUEST))
        return context
    }
    private val scopes = listOf(
        "https://www.googleapis.com/auth/userinfo.email",
        "https://www.googleapis.com/auth/userinfo.profile",
        "https://www.googleapis.com/auth/gmail.readonly",
        "https://www.googleapis.com/auth/gmail.modify",
        "https://www.googleapis.com/auth/gmail.compose",
        "https://www.googleapis.com/auth/gmail.send",
        "https://www.googleapis.com/auth/gmail.insert",
        "https://www.googleapis.com/auth/gmail.labels",
        "https://www.googleapis.com/auth/gmail.metadata",
        "https://www.googleapis.com/auth/gmail.settings.basic",
        "https://www.googleapis.com/auth/gmail.settings.sharing",
        "https://www.googleapis.com/auth/gmail.addons.current.message.action",
        "https://www.googleapis.com/auth/gmail.addons.current.message.metadata",
        "https://www.googleapis.com/auth/gmail.addons.current.message.readonly",
        "https://www.googleapis.com/auth/gmail.addons.current.action.compose",
        "https://mail.google.com/",
       "https://www.googleapis.com/auth/drive",
       "https://www.googleapis.com/auth/drive.file",
       "https://www.googleapis.com/auth/drive.appdata",
       "https://www.googleapis.com/auth/drive.metadata.readonly",
       "https://www.googleapis.com/auth/calendar",
       "https://www.googleapis.com/auth/calendar.events",
       "https://www.googleapis.com/auth/calendar.events.readonly",
       "https://www.googleapis.com/auth/calendar.readonly",
    )

    private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val flow = GoogleAuthorizationCodeFlow.Builder(
        httpTransport,
        jsonFactory,
        GoogleClientSecrets.load(
            jsonFactory,
            InputStreamReader(key()!!)
        ),
        scopes
    ).build()

    private inner class LoginServlet : HttpServlet() {
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


    private inner class CallbackServlet : HttpServlet() {
        override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
            val code = req.getParameter("code")
            if (code != null) {
                val credential = flow.createAndStoreCredential(
                    flow.newTokenRequest(code).setRedirectUri(redirectUri).execute(), null
                )
                val userInfo = Oauth2.Builder(
                    httpTransport,
                    jsonFactory,
                    credential
                ).setApplicationName(applicationName).build().userinfo().get().execute()
                val user = User(
                    id = userInfo.id,
                    email = userInfo.email,
                    name = userInfo.name,
                    picture = userInfo.picture,
                    credential = credential,
                )
                val sessionID = UUID.randomUUID().toString()
                ApplicationServices.authenticationManager.putUser(accessToken = sessionID, user = user)
                log.info("User $user logged in with session $sessionID")
                val sessionCookie = Cookie(AuthenticationInterface.AUTH_COOKIE, sessionID)
                sessionCookie.path = "/"
                sessionCookie.isHttpOnly = true
                sessionCookie.secure = true
                sessionCookie.maxAge = TimeUnit.DAYS.toSeconds(1).toInt()
                sessionCookie.comment = "Authentication Session ID"
                resp.addCookie(sessionCookie)
                val redirect = req.getParameter("state")?.urlDecode()
                resp.sendRedirect(redirect ?: "/")
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Authorization code not found")
            }
        }
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(OAuthGoogle::class.java)

        fun String.urlDecode(): String = try {
            URLDecoder.decode(this, StandardCharsets.UTF_8.toString())
        } catch (e: UnsupportedEncodingException) {
            this
        }

    }

}

