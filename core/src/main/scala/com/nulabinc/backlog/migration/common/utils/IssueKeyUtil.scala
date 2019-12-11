package com.nulabinc.backlog.migration.common.utils

import scala.util.matching.Regex

/**
  * @author uchida
  */
object IssueKeyUtil {

  def replace(issueKey: String, projectKey: String): String = {
    """(^[0-9A-Z_]+)""".r.replaceFirstIn(issueKey, projectKey)
  }

  def findIssueIndex(issueKey: String): Int = {
    val pattern: Regex      = """^[0-9A-Z_]+-(\d+)$""".r
    val pattern(issueIndex) = issueKey
    issueIndex.toInt
  }

}
