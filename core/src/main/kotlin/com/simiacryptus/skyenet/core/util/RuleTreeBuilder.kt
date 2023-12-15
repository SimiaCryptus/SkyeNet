package com.simiacryptus.skyenet.core.util

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

  fun getRuleExpression(
    toMatch: Set<String>,
    doNotMatch: SortedSet<String>,
    result: Boolean
  ): String {
    if(doNotMatch.size < toMatch.size) return getRuleExpression(doNotMatch, toMatch.toSortedSet(), !result)
    val sb = StringBuilder()
    sb.append("when {\n")
    sb.append("  " + getRules(toMatch.toSet(), doNotMatch.toSortedSet(), result).replace("\n", "\n  ") + "\n")
    sb.append("  else -> ${!result}\n")
    sb.append("}")
    return sb.toString()
  }

  private fun getRules(
    toMatch: Set<String>,
    doNotMatch: SortedSet<String>,
    result: Boolean
  ) : String {
    val sb: StringBuilder = StringBuilder()
    if (doNotMatch.isEmpty()) {
      sb.append("true -> $result\n")
      return sb.toString()
    }
    val remainingItems = toMatch.toMutableSet()
    while (remainingItems.isNotEmpty()) {
      val sortedItems = remainingItems.toSortedSet()

      fun String.bestPrefix() = allowedPrefixes(setOf(this), doNotMatch).firstOrNull() ?: this
      val bestNextPrefix = bestNextPrefix(remainingItems, doNotMatch, sortedItems)

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
        bestNextPrefix.first.isEmpty() -> break
        else -> {
          val matchedItems = remainingItems.filter { it.startsWith(bestNextPrefix.first) }.toSet()
          val matchedPrefixes = matchedItems.map { it.bestPrefix() }.toSet()
          val matchedBlacklist = doNotMatch.filter { it.startsWith(bestNextPrefix.first) }
          when {
            matchedPrefixes.size < 5 -> break
            matchedBlacklist.isEmpty() -> sb.append("""path.startsWith("${bestNextPrefix.first.bestPrefix().escape}") -> $result""" + "\n")
            else -> {
              val subRules = getRuleExpression(
                matchedItems.map { it.removePrefix(bestNextPrefix.first) }.toSet(),
                matchedBlacklist.map { it.removePrefix(bestNextPrefix.first) }.toSortedSet(),
                result
              )
              sb.append(
                """
                path.startsWith("${bestNextPrefix.first.escape}") -> {
                  val path = path.removePrefix("${bestNextPrefix.first.escape}")
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
    remainingItems.flatMap { allowedPrefixes(setOf(it), doNotMatch) }
      .distinct().sorted().forEach { sb.append("""path.startsWith("${it.escape}") -> $result""" + "\n") }
    return sb.toString()
  }

  fun bestNextPrefix(
    remainingItems: MutableSet<String>,
    doNotMatch: SortedSet<String>,
    sortedItems: SortedSet<String>
  ): Pair<String, Int>? {
    val remainingAllowedPrefixes = allowedPrefixes(remainingItems, doNotMatch)
    val bestNextPrefix = prefixExpand(remainingAllowedPrefixes).map { prefix ->
      val matchingItems = sortedItems.subSet(prefix, prefix + "\uFFFF")
      prefix to matchingItems.sumOf { prefix.length } - prefix.length
    }.maxByOrNull { it.second }
    return bestNextPrefix
  }

  fun bestNextSuffix(
    remainingItems: MutableSet<String>,
    doNotMatchReversed: SortedSet<String>,
    sortedItems: SortedSet<String>
  ) = prefixExpand(allowedPrefixes(remainingItems.map { it.reversed() }, doNotMatchReversed)).map { it.reversed() }.map { prefix ->
      val matchingItems = sortedItems.subSet(prefix, prefix + "\uFFFF")
      prefix to matchingItems.sumOf { prefix.length } - prefix.length
    }.maxByOrNull { it.second }

  fun prefixExpand(allowedPrefixes: Collection<String>) =
    allowedPrefixes.filter { allowedPrefixes.none { prefix -> prefix != it && prefix.startsWith(it) } }
      .flatMap { (1..it.length).map { i -> it.substring(0, i) } }.toSet()

  fun allowedPrefixes(
    items: Collection<String>,
    doNotMatch: SortedSet<String>
  ) = items.toList().parallelStream().map { item ->
    val list = listOf(
      item.safeSubstring(0, longestCommonPrefix(doNotMatch.tailSet(item).firstOrNull(), item)?.length?.let { it+1 }),
      item.safeSubstring(0, longestCommonPrefix(doNotMatch.headSet(item).lastOrNull(), item)?.length?.let { it+1 }),
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


