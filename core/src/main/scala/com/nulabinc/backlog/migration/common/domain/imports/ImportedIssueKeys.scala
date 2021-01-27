package com.nulabinc.backlog.migration.common.domain.imports

case class ImportedIssueKeys(
    srcIssueId: Long,
    optSrcIssueIndex: Option[Long],
    dstIssueId: Long,
    optDstIssueIndex: Option[Long]
)

object ImportedIssueKeys {
  val empty: ImportedIssueKeys = ImportedIssueKeys(0, None, 0, None)
}
