package com.simiacryptus.diff

import java.io.File
import java.nio.file.Path
import java.util.*

class FileValidationUtils {
    companion object {
        fun isCurlyBalanced(code: String): Boolean {
            var count = 0
            for (char in code) {
                when (char) {
                    '{' -> count++
                    '}' -> count--
                }
                if (count < 0) return false
            }
            return count == 0
        }

        fun isSingleQuoteBalanced(code: String): Boolean {
            var count = 0
            var escaped = false
            for (char in code) {
                when {
                    char == '\\' -> escaped = !escaped
                    char == '\'' && !escaped -> count++
                    else -> escaped = false
                }
            }
            return count % 2 == 0
        }

        fun isSquareBalanced(code: String): Boolean {
            var count = 0
            for (char in code) {
                when (char) {
                    '[' -> count++
                    ']' -> count--
                }
                if (count < 0) return false
            }
            return count == 0
        }

        fun isParenthesisBalanced(code: String): Boolean {
            var count = 0
            for (char in code) {
                when (char) {
                    '(' -> count++
                    ')' -> count--
                }
                if (count < 0) return false
            }
            return count == 0
        }

        fun isQuoteBalanced(code: String): Boolean {
            var count = 0
            var escaped = false
            for (char in code) {
                when {
                    char == '\\' -> escaped = !escaped
                    char == '"' && !escaped -> count++
                    else -> escaped = false
                }
            }
            return count % 2 == 0
        }

        fun filteredWalk(file: File, fn: (File) -> Boolean) : List<File> {
            val result = mutableListOf<File>()
            if (fn(file)) {
                if (file.isDirectory) {
                    file.listFiles()?.forEach { child ->
                        result.addAll(filteredWalk(child, fn))
                    }
                } else {
                    result.add(file)
                }
            }
            return result
        }

        fun isLLMIncludable(file: File) : Boolean {
            return when {
                !file.exists() -> false
                file.isDirectory -> false
                file.name.startsWith(".") -> false
                file.length() > (256 * 1024) -> false
                isGitignore(file.toPath()) -> false
                file.extension.lowercase(Locale.getDefault()) in setOf(
                    "jar",
                    "zip",
                    "class",
                    "png",
                    "jpg",
                    "jpeg",
                    "gif",
                    "ico",
                    "stl"
                ) -> false
                else -> true
            }
        }

        fun expandFileList(vararg data: File): Array<File> {
            return data.flatMap {
                (when {
                    it.name.startsWith(".") -> arrayOf()
                    isGitignore(it.toPath()) -> arrayOf()
                    it.length() > 1e6 -> arrayOf()
                    it.extension.lowercase(Locale.getDefault()) in
                            setOf("jar", "zip", "class", "png", "jpg", "jpeg", "gif", "ico") -> arrayOf()
                    it.isDirectory -> expandFileList(*it.listFiles() ?: arrayOf())
                    else -> arrayOf(it)
                }).toList()
            }.toTypedArray()
        }

        fun isGitignore(path: Path): Boolean {
            var currentDir = path.toFile().parentFile
            currentDir ?: return false
            while (!currentDir.resolve(".git").exists()) {
                currentDir.resolve(".gitignore").let {
                    if (it.exists()) {
                        val gitignore = it.readText()
                        if (gitignore.split("\n").any { line ->
                                val pattern = line.trim().trimEnd('/').replace(".", "\\.").replace("*", ".*")
                                line.trim().isNotEmpty()
                                        && !line.startsWith("#")
                                        && path.fileName.toString().trimEnd('/').matches(Regex(pattern))
                            }) {
                            return true
                        }
                    }
                }
                currentDir = currentDir.parentFile ?: return false
            }
            currentDir.resolve(".gitignore").let {
                if (it.exists()) {
                    val gitignore = it.readText()
                    if (gitignore.split("\n").any { line ->
                            val pattern = line.trim().trimEnd('/').replace(".", "\\.").replace("*", ".*")
                            line.trim().isNotEmpty()
                                    && !line.startsWith("#")
                                    && path.fileName.toString().trimEnd('/').matches(Regex(pattern))
                        }) {
                        return true
                    }
                }
            }
            return false
        }

    }

}