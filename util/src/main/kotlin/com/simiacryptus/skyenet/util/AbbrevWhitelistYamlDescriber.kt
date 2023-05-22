package com.simiacryptus.skyenet.util

import com.simiacryptus.util.YamlDescriber

class AbbrevWhitelistYamlDescriber(vararg val abbreviated: String) : YamlDescriber() {
    override fun isAbbreviated(name: String) = abbreviated.find { name.startsWith(it) } == null
}