package com.simiacryptus.skyenet.webui

import com.simiacryptus.skyenet.core.actors.CodingActor
import com.simiacryptus.skyenet.core.actors.ImageActor
import com.simiacryptus.skyenet.core.actors.ParsedActor
import com.simiacryptus.skyenet.core.actors.SimpleActor
import com.simiacryptus.skyenet.core.platform.ApplicationServices
import com.simiacryptus.skyenet.core.platform.AuthenticationManager
import com.simiacryptus.skyenet.core.platform.AuthorizationManager
import com.simiacryptus.skyenet.core.platform.User
import com.simiacryptus.skyenet.groovy.GroovyInterpreter
import com.simiacryptus.skyenet.kotlin.KotlinInterpreter
import com.simiacryptus.skyenet.scala.ScalaLocalInterpreter
import com.simiacryptus.skyenet.webui.test.CodingActorTestApp
import com.simiacryptus.skyenet.webui.test.ImageActorTestApp
import com.simiacryptus.skyenet.webui.test.ParsedActorTestApp
import com.simiacryptus.skyenet.webui.test.SimpleActorTestApp
import java.util.function.Function


object ActorTestAppServer : com.simiacryptus.skyenet.webui.application.ApplicationDirectory(port = 8082) {

    data class TestJokeDataStructure(
        val setup: String? = null,
        val punchline: String? = null,
        val type: String? = null,
    )

    interface JokeParser : Function<String, TestJokeDataStructure>

    override val childWebApps by lazy {
        listOf(
            ChildWebApp("/test_simple", SimpleActorTestApp(SimpleActor("Translate the user's request into pig latin.", "PigLatin"))),
            ChildWebApp("/test_parsed_joke", ParsedActorTestApp(ParsedActor(JokeParser::class.java, "Tell me a joke"))),
            ChildWebApp("/images", ImageActorTestApp(ImageActor())),
            ChildWebApp("/test_coding_scala", CodingActorTestApp(CodingActor(ScalaLocalInterpreter::class))),
            ChildWebApp("/test_coding_kotlin", CodingActorTestApp(CodingActor(KotlinInterpreter::class))),
            ChildWebApp("/test_coding_groovy", CodingActorTestApp(CodingActor(GroovyInterpreter::class))),
        )}

    @JvmStatic
    fun main(args: Array<String>) {
        val mockUser = User(
            "1",
            "user@mock.test",
            "Test User",
            ""
        )
        ApplicationServices.authenticationManager = object : AuthenticationManager() {
            override fun getUser(sessionId: String?) = mockUser
            override fun containsUser(value: String) = true
            override fun putUser(sessionId: String, user: User) = throw UnsupportedOperationException()
        }
        ApplicationServices.authorizationManager = object : AuthorizationManager() {
            override fun isAuthorized(
                applicationClass: Class<*>?,
                user: User?,
                operationType: OperationType
            ): Boolean = true
        }
        super._main(args)
    }
}

