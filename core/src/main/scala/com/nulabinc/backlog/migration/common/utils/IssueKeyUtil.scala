package com.nulabinc.backlog.migration.common.utils



/**
 * @author uchida
 */
object IssueKeyUtil {
  def replace(issueKey: String, projectKey: String): String = {
    """(^[0-9A-Z_]+)""".r.replaceFirstIn(issueKey, projectKey)
  }
}
