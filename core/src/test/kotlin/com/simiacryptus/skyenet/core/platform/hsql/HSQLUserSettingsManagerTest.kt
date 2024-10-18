package com.simiacryptus.skyenet.core.platform.hsql

import com.simiacryptus.skyenet.core.platform.test.UsageTest
import java.nio.file.Files

class HSQLUsageManagerTest : UsageTest(HSQLUsageManager(Files.createTempDirectory("usageManager").toFile()))
