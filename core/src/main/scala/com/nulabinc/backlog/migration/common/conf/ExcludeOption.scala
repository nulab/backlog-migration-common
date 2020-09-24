package com.nulabinc.backlog.migration.common.conf

case class ExcludeOption(issue: Boolean, wiki: Boolean) {
  override def toString: String = {
    val issueArr = if (issue) Seq("issue") else Seq()
    val wikiArr  = if (wiki) Seq("wiki") else Seq()

    (issueArr ++ wikiArr).mkString(", ")
  }
}

object ExcludeOption {
  val default: ExcludeOption = ExcludeOption(issue = false, wiki = false)
}
