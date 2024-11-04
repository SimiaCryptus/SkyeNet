package com.simiacryptus.skyenet.apps.parse

import com.simiacryptus.jopenai.API

interface ParsingModel {
  fun merge(runningDocument: DocumentData, newData: DocumentData): DocumentData
  fun getParser(api: API): (String) -> DocumentData
  fun newDocument(): DocumentData

  interface DocumentMetadata
  interface ContentData {
    val type: String
    val text: String?
    val content_list: List<ContentData>?
    val tags: List<String>?
  }

  interface DocumentData {
    val id: String?
    val content_list: List<ContentData>?
//        val metadata: DocumentMetadata?
  }

  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(ParsingModel::class.java)
  }
}