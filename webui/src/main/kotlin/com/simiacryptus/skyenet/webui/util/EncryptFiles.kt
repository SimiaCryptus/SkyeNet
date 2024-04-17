package com.simiacryptus.skyenet.webui.util

import com.simiacryptus.skyenet.core.platform.ApplicationServices
import java.nio.file.Files
import java.nio.file.Paths

object EncryptFiles {

    @JvmStatic
    fun main(args: Array<String>) {
        "".encrypt("arn:aws:kms:us-east-1:470240306861:key/a1340b89-64e6-480c-a44c-e7bc0c70dcb1")
            .write("""C:\Users\andre\code\SkyenetApps\src\main\resources\patreon.json.kms""")
    }

}

fun String.write(outpath: String) {
    Files.write(Paths.get(outpath), toByteArray())
}

fun String.encrypt(keyId: String) = ApplicationServices.cloud!!.encrypt(encodeToByteArray(), keyId)
    ?: throw RuntimeException("Unable to encrypt data")