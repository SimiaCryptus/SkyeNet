package com.simiacryptus.skyenet.apps.parsers

import com.simiacryptus.jopenai.API

interface ParsingModel {
    fun merge(runningDocument: DocumentData, newData: DocumentData): DocumentData
    fun getParser(api: API): (String) -> DocumentData
    fun newDocument(): DocumentData

    interface DocumentMetadata
    interface ContentData {
        val type: String
        val text: String?
        val content: List<ContentData>?
        val tags: List<String>?
    }

    interface DocumentData {
        val id: String?
        val content: List<ContentData>?
        val metadata: DocumentMetadata?
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(ParsingModel::class.java)
    }
}