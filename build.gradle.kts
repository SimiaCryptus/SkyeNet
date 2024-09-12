fun properties(key: String) = project.findProperty(key).toString()
group = properties("libraryGroup")
version = properties("libraryVersion")

//plugins {
//    id("org.jetbrains.kotlin.jvm") version "2.0.20"
//}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion")
    }
}
