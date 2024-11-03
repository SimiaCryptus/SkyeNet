package com.simiacryptus.skyenet.core.platform

import com.simiacryptus.skyenet.core.platform.file.*
import com.simiacryptus.skyenet.core.platform.hsql.HSQLMetadataStorage
import com.simiacryptus.skyenet.core.platform.hsql.HSQLUsageManager
import com.simiacryptus.skyenet.core.platform.model.ApplicationServicesConfig.dataStorageRoot
import com.simiacryptus.skyenet.core.platform.model.ApplicationServicesConfig.isLocked
import com.simiacryptus.skyenet.core.platform.model.AuthenticationInterface
import com.simiacryptus.skyenet.core.platform.model.AuthorizationInterface
import com.simiacryptus.skyenet.core.platform.model.CloudPlatformInterface
import com.simiacryptus.skyenet.core.platform.model.MetadataStorageInterface
import com.simiacryptus.skyenet.core.platform.model.StorageInterface
import com.simiacryptus.skyenet.core.platform.model.UsageInterface
import com.simiacryptus.skyenet.core.platform.model.UserSettingsInterface
import com.simiacryptus.skyenet.core.util.Selenium
import java.io.File
import java.util.concurrent.ThreadPoolExecutor

object ApplicationServices {
  var authorizationManager: AuthorizationInterface = AuthorizationManager()
    set(value) {
      require(!isLocked) { "ApplicationServices is locked" }
      field = value
    }
  var userSettingsManager: UserSettingsInterface = UserSettingsManager()
    set(value) {
      require(!isLocked) { "ApplicationServices is locked" }
      field = value
    }
  var authenticationManager: AuthenticationInterface = AuthenticationManager()
    set(value) {
      require(!isLocked) { "ApplicationServices is locked" }
      field = value
    }
  var dataStorageFactory: (File) -> StorageInterface = { DataStorage(it) }
    set(value) {
      require(!isLocked) { "ApplicationServices is locked" }
      field = value
    }
  var metadataStorageFactory: (File) -> MetadataStorageInterface = { HSQLMetadataStorage(it) }
    set(value) {
      require(!isLocked) { "ApplicationServices is locked" }
      field = value
    }
  var clientManager: ClientManager = ClientManager()
    set(value) {
      require(!isLocked) { "ApplicationServices is locked" }
      field = value
    }

  var cloud: CloudPlatformInterface? = AwsPlatform.get()
    set(value) {
      require(!isLocked) { "ApplicationServices is locked" }
      field = value
    }


  var seleniumFactory: ((ThreadPoolExecutor, Array<out jakarta.servlet.http.Cookie>?) -> Selenium)? = null
    set(value) {
      require(!isLocked) { "ApplicationServices is locked" }
      field = value
    }
  var usageManager: UsageInterface = HSQLUsageManager(File(dataStorageRoot, "usage"))
    set(value) {
      require(!isLocked) { "ApplicationServices is locked" }
      field = value
    }

}