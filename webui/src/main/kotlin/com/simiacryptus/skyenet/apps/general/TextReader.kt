package com.simiacryptus.skyenet.apps.general

import java.awt.image.BufferedImage
import java.io.File

class TextReader(private val textFile: File) : DocumentParserApp.DocumentReader {
    private val content: List<String> = textFile.readLines()

    override fun getPageCount(): Int = 1

    override fun getText(startPage: Int, endPage: Int): String {
        return content.joinToString("\n")
    }

    override fun renderImage(pageIndex: Int, dpi: Float): BufferedImage {
        throw UnsupportedOperationException("Text files do not support image rendering")
    }

    override fun close() {
        // No resources to close for text files
    }
}