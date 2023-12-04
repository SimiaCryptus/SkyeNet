package com.simiacryptus.skyenet.core.platform

import com.simiacryptus.skyenet.core.platform.file.DataStorage
import com.simiacryptus.skyenet.core.platform.test.StorageInterfaceTest
import java.nio.file.Files

class DataStorageTest : StorageInterfaceTest(DataStorage(Files.createTempDirectory("sessionDataTest").toFile()))

