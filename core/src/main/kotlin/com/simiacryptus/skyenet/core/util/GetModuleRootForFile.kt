package com.simiacryptus.skyenet.core.util

import java.io.File

fun getModuleRootForFile(file: File): File {
    var current = file
    while (current.parentFile != null) {
        if (current.resolve(".git").exists()) {
            return current
        }
        current = current.parentFile
    }
    return file
}