fun properties(key: String) = project.findProperty(key).toString()
group = properties("libraryGroup")
version = properties("libraryVersion")

//plugins {
//    id("org.jetbrains.kotlin.jvm") version "1.9.21"
//}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion")
    }
}
