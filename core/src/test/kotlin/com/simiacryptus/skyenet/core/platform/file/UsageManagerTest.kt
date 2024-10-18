package com.simiacryptus.skyenet.core.platform.file

import com.simiacryptus.skyenet.core.platform.test.UsageTest
import java.nio.file.Files

class UsageManagerTest : UsageTest(UsageManager(Files.createTempDirectory("usageManager").toFile()))