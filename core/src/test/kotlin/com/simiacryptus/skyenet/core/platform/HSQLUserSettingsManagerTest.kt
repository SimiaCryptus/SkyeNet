package com.simiacryptus.skyenet.core.platform

import com.simiacryptus.skyenet.core.platform.file.UsageManager
import com.simiacryptus.skyenet.core.platform.file.UserSettingsManager
import com.simiacryptus.skyenet.core.platform.test.UsageTest
import com.simiacryptus.skyenet.core.platform.test.UserSettingsTest
import java.nio.file.Files

class HSQLUsageManagerTest : UsageTest(HSQLUsageManager(Files.createTempDirectory("usageManager").toFile()))
