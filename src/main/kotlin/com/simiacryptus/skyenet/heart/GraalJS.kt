//@file:Suppress("unused")
//
//package com.simiacryptus.skyenet.heart
//
//
//import org.graalvm.polyglot.*
//
//open class GraalJS(
//    private val prefix: String = "",
//    defs: Map<String, Any> = mapOf(),
//) {
//    private val engine: Context by lazy {
//        Context.newBuilder("js")
//            .allowHostAccess(
//                HostAccess.newBuilder(HostAccess.ALL)
//                    .allowPublicAccess(true)
//                    .allowAllImplementations(true)
//                    .build()
//            )
//            .allowAllAccess(true)
//            .build()
//    }
//    init {
//        defs.forEach { (key, value) ->
//            engine.polyglotBindings.putMember(key, value)
//            engine.getBindings("js").putMember(key, value)
//        }
//    }
//
//    fun run(jsCode: String): Value {
//        val source = Source.create("js", prefix + "\n" + jsCode)
//        return engine.eval(source)
//    }
//}
//
//
//
