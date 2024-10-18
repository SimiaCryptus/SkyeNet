package com.simiacryptus.skyenet.apps.parsers

import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.abs

class TextReader(private val textFile: File) : DocumentParserApp.DocumentReader {
    private val pages: List<String> = splitIntoPages(textFile.readLines().joinToString("\n"))

    override fun getPageCount(): Int = pages.size

    override fun getText(startPage: Int, endPage: Int): String {
        return pages.subList(startPage, endPage.coerceAtMost(pages.size)).joinToString("\n")
    }

    override fun renderImage(pageIndex: Int, dpi: Float): BufferedImage {
        throw UnsupportedOperationException("Text files do not support image rendering")
    }

    override fun close() {
        // No resources to close for text files
    }

    private fun splitIntoPages(text: String, maxChars: Int = 16000): List<String> {
        if (text.length <= maxChars) return listOf(text)
        val lines = text.split("\n")
        var bestSplitIndex = lines.size / 2
        var bestFitness = Double.MAX_VALUE
        for (i in lines.indices) {
            val leftSize = lines.subList(0, i).joinToString("\n").length
            val rightSize = lines.subList(i, lines.size).joinToString("\n").length
            val fitness = abs(leftSize - rightSize) + (if (lines[i].isEmpty()) 0 else 1000)
            if (fitness < bestFitness) {
                bestFitness = fitness.toDouble()
                bestSplitIndex = i
            }
        }
        val leftText = lines.subList(0, bestSplitIndex).joinToString("\n")
        val rightText = lines.subList(bestSplitIndex, lines.size).joinToString("\n")
        return splitIntoPages(leftText, maxChars) + splitIntoPages(rightText, maxChars)
    }
}