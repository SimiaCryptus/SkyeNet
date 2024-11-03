fun properties(key: String) = project.findProperty(key).toString()
group = properties("libraryGroup")
version = properties("libraryVersion")

tasks {
  wrapper {
    gradleVersion = properties("gradleVersion")
  }
}
