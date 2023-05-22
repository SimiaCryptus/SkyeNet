package com.simiacryptus.skyenet.util

import com.simiacryptus.util.YamlDescriber

class AbbrevBlacklistYamlDescriber(vararg val abbreviated: String) : YamlDescriber() {
    override fun isAbbreviated(name: String) = abbreviated.find { name.startsWith(it) } != null
}

