package com.simiacryptus.skyenet

import com.simiacryptus.skyenet.actors.CodingActor
import com.simiacryptus.skyenet.actors.ParsedActor
import com.simiacryptus.skyenet.actors.SimpleActor
import com.simiacryptus.skyenet.heart.GroovyInterpreter
import com.simiacryptus.skyenet.heart.KotlinInterpreter
import com.simiacryptus.skyenet.heart.ScalaLocalInterpreter
import com.simiacryptus.skyenet.test.CodingActorTestApp
import com.simiacryptus.skyenet.test.ParsedActorTestApp
import com.simiacryptus.skyenet.test.SimpleActorTestApp
import java.util.function.Function


object ActorTestAppServer : ApplicationDirectory(port = 8082) {

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
            ChildWebApp("/test_coding_scala", CodingActorTestApp(CodingActor(ScalaLocalInterpreter::class))),
            ChildWebApp("/test_coding_kotlin", CodingActorTestApp(CodingActor(KotlinInterpreter::class))),
            ChildWebApp("/test_coding_groovy", CodingActorTestApp(CodingActor(GroovyInterpreter::class))),
        )}

    @JvmStatic
    fun main(args: Array<String>) {
        super._main(args)
    }
}

