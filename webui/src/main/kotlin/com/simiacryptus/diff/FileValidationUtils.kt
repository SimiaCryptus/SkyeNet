package com.simiacryptus.diff

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
    }

}