package com.simiacryptus.skyenet.core.util

import org.intellij.lang.annotations.Language
import java.util.*
import java.util.stream.Collectors

object RuleTreeBuilder {

  val String.escape get() = replace("$", "\\$")

  fun String.safeSubstring(from: Int, to: Int?) = when {
    to == null -> ""
    from >= to -> ""
    from < 0 -> ""
    to > length -> ""
    else -> substring(from, to)
  }

  @Language("kotlin")
  fun getRuleExpression(
    toMatch: Set<String>,
    doNotMatch: SortedSet<String>,
    result: Boolean
  ): String = if (doNotMatch.size < toMatch.size) {
    getRuleExpression(doNotMatch, toMatch.toSortedSet(), !result)
  } else """
      when {
        ${getRules(toMatch.toSet(), doNotMatch.toSortedSet(), result).replace("\n", "\n  ")}
        else -> ${!result}
      }        
      """.trimIndent().trim()

  private fun getRules(
    toMatch: Set<String>,
    doNotMatch: SortedSet<String>,
    result: Boolean
  ): String {
    if (doNotMatch.isEmpty()) return "true -> $result\n"
    val sb: StringBuilder = StringBuilder()
    val remainingItems = toMatch.toMutableSet()
    fun String.bestPrefix(): String {
      val pfx = allowedPrefixes(setOf(this), doNotMatch).firstOrNull() ?: this
      require(pfx.isNotBlank())
      require(doNotMatch.none { it.startsWith(pfx) })
      return pfx
    }
    while (remainingItems.isNotEmpty()) {

      val bestNextPrefix = bestPrefix(remainingItems.toSortedSet(), doNotMatch)

//      val doNotMatchReversed = remainingItems.map { it.reversed() }.toSortedSet()
//      fun String.bestSuffix() = allowedPrefixes(setOf(this).map { it.reversed() }, doNotMatchReversed).firstOrNull()?.reversed() ?: this
//      val bestNextSuffix = bestNextSuffix(remainingItems, doNotMatchReversed, sortedItems)

      when {
//        bestNextSuffix != null && bestNextSuffix.second > (bestNextPrefix?.second ?: 0) -> {
//          val matchedItems = remainingItems.filter { it.endsWith(bestNextSuffix.first) }.toSet()
//          val matchedSuffixes = matchedItems.map { it.bestSuffix() }.toSet()
//          val matchedBlacklist = doNotMatch.filter { it.endsWith(bestNextSuffix.first) }
//          when {
//            matchedSuffixes.size < 5 -> break
//            matchedBlacklist.isEmpty() -> sb.append("""path.endsWith("${bestNextSuffix.first.bestSuffix().escape}") -> $result""" + "\n")
//            else -> {
//              val subRules = getRuleExpression(
//                matchedItems.map { it.removeSuffix(bestNextSuffix.first) }.toSet(),
//                matchedBlacklist.map { it.removeSuffix(bestNextSuffix.first) }.toSortedSet(),
//                result
//              )
//              sb.append(
//                """
//                path.endsWith("${bestNextSuffix.first.escape}") -> {
//                  val path = path.removeSuffix("${bestNextSuffix.first.bestSuffix().escape}")
//                  ${subRules.replace("\n", "\n  ")}
//                }
//                """.trimIndent() + "\n"
//              )
//            }
//          }
//          remainingItems.removeAll(matchedItems)
//        }
        bestNextPrefix == null -> break
        else -> {
          val matchedItems = remainingItems.filter { it.startsWith(bestNextPrefix) }.toSet()
          val matchedBlacklist = doNotMatch.filter { it.startsWith(bestNextPrefix) }
          when {
            matchedBlacklist.isEmpty() -> sb.append("""path.startsWith("${bestNextPrefix.bestPrefix().escape}") -> $result""" + "\n")
            matchedItems.map { it.bestPrefix() }.toSet().size < 3 -> break
            else -> {
              val subRules = getRuleExpression(
                matchedItems.map { it.removePrefix(bestNextPrefix) }.toSet(),
                matchedBlacklist.map { it.removePrefix(bestNextPrefix) }.toSortedSet(),
                result
              )
              sb.append(
                """
                path.startsWith("${bestNextPrefix.escape}") -> {
                  val path = path.substring(${bestNextPrefix.length})
                  ${subRules.replace("\n", "\n  ")}
                }
                """.trimIndent() + "\n"
              )
            }
          }
          remainingItems.removeAll(matchedItems)
        }
      }
    }
    remainingItems.map { it.bestPrefix() }.toSortedSet().forEach {
      require(doNotMatch.none { prefix -> prefix.startsWith(it) })
      sb.append("""path.startsWith("${it.escape}") -> $result""" + "\n")
    }
    return sb.toString()
  }

  fun bestPrefix(
    toMatch: SortedSet<String>,
    doNotMatch: SortedSet<String>
  ) = allowedPrefixes(toMatch, doNotMatch)
//    .flatMap { prefixExpand(listOf(it)) }
    .filter { it.isNotBlank() }
    .maxByOrNull { prefix ->
      val good = toMatch.subSet(prefix, prefix + "\uFFFF")
      val bad = doNotMatch.subSet(prefix, prefix + "\uFFFF")
      val goodCnt = good.size
      val badCnt = bad.size
      if(badCnt == 0) return@maxByOrNull goodCnt.toDouble()
      if(goodCnt == 0) return@maxByOrNull badCnt.toDouble()
      val totalCnt = goodCnt + badCnt
      val goodFactor = goodCnt.toDouble() / totalCnt
      val badFactor = badCnt.toDouble() / totalCnt
      goodFactor * Math.log(goodFactor) + badFactor * Math.log(badFactor)
    }

  fun bestNextSuffix(
    remainingItems: MutableSet<String>,
    doNotMatchReversed: SortedSet<String>,
    sortedItems: SortedSet<String>
  ) = prefixExpand(allowedPrefixes(remainingItems.map { it.reversed() }, doNotMatchReversed)).map { it.reversed() }
    .map { prefix ->
      val matchingItems = sortedItems.subSet(prefix, prefix + "\uFFFF")
      prefix to matchingItems.sumOf { prefix.length } - prefix.length
    }.maxByOrNull { it.second }

  fun prefixExpand(allowedPrefixes: Collection<String>) =
    allowedPrefixes.filter { allowedPrefixes.none { prefix -> prefix != it && prefix.startsWith(it) } }
      .flatMap { prefixExpand(it) }.toSet()

  fun prefixExpand(it: String) = (1..it.length).map { i -> it.substring(0, i) }

  fun allowedPrefixes(
    items: Collection<String>,
    doNotMatch: SortedSet<String>
  ) = items.toList().parallelStream().map { item ->
    val list = listOf(
      item.safeSubstring(0, longestCommonPrefix(doNotMatch.tailSet(item).firstOrNull(), item)?.length?.let { it + 1 }),
      item.safeSubstring(0, longestCommonPrefix(doNotMatch.headSet(item).lastOrNull(), item)?.length?.let { it + 1 }),
    )
    list.maxByOrNull { it.length } ?: list.firstOrNull()
  }.distinct().collect(Collectors.toSet()).filterNotNull().filter { it.isNotBlank() }.toSortedSet()

  fun longestCommonPrefix(a: String?, b: String?): String? {
    if (a == null || b == null) return null
    var i = 0
    while (i < a.length && i < b.length && a[i] == b[i]) i++
    return a.substring(0, i)
  }

}


