package com.simiacryptus.skyenet.core.platform.file

import com.simiacryptus.skyenet.core.platform.test.MetadataStorageInterfaceTest
import java.nio.file.Files

class MetadataStorageTest : MetadataStorageInterfaceTest(MetadataStorage(Files.createTempDirectory("sessionMetadataTest").toFile()))