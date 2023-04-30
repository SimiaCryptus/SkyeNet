package com.simiacryptus.skyenet.util

import com.simiacryptus.util.YamlDescriber

class AbbrevWhitelistYamlDescriber(vararg val abbreviated: String) : YamlDescriber() {
    override fun toYaml(
        rawType: Class<in Nothing>,
        stackMax: Int,
    ): String {

        val name = rawType.name
        if (abbreviated.find { name.startsWith(it) } == null)
            return """
            |type: object
            |class: $name
            """.trimMargin()
        return super.toYaml(rawType, stackMax)
    }
}