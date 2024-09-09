package com.simiacryptus.skyenet.apps.general.parsers

import com.simiacryptus.jopenai.API

interface ParsingModel {
    fun merge(runningDocument: DocumentData, newData: DocumentData): DocumentData
    fun getParser(api: API): (String) -> DocumentData
    fun newDocument(): DocumentData

    interface DocumentData
    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(ParsingModel::class.java)
    }
}