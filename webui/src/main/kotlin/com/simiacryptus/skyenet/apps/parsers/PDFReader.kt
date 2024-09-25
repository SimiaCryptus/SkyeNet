package com.simiacryptus.skyenet.apps.parsers

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import java.awt.image.BufferedImage
import java.io.File

class PDFReader(pdfFile: File) : DocumentParserApp.DocumentReader {
    private val document: PDDocument = PDDocument.load(pdfFile)
    private val renderer: PDFRenderer = PDFRenderer(document)
    private val stripper: PDFTextStripper = PDFTextStripper().apply { sortByPosition = true }

    override fun getPageCount(): Int = document.numberOfPages

    override fun getText(startPage: Int, endPage: Int): String {
        stripper.startPage = startPage
        stripper.endPage = endPage
        return stripper.getText(document)
    }

    override fun renderImage(pageIndex: Int, dpi: Float): BufferedImage {
        return renderer.renderImageWithDPI(pageIndex, dpi)
    }

    override fun close() {
        document.close()
    }
}