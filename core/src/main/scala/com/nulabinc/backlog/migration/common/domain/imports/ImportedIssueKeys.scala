package com.nulabinc.backlog.migration.common.domain.imports

case class ImportedIssueKeys(
    srcIssueId: Long,
    srcIssueIndex: Int,
    dstIssueId: Long,
    dstIssueIndex: Int
)

object ImportedIssueKeys {
  val empty: ImportedIssueKeys = ImportedIssueKeys(0, 0, 0, 0)
}
