package com.nulabinc.backlog.migration.common.persistence.store.sqlite.ops

import com.nulabinc.backlog.migration.common.domain.Types.AnyId
import com.nulabinc.backlog.migration.common.domain._
import com.nulabinc.backlog.migration.common.domain.exports.{
  DeletedExportedBacklogStatus,
  ExistingExportedBacklogStatus,
  ExportedBacklogStatus
}
import doobie._
import doobie.implicits._
import com.nulabinc.backlog.migration.common.domain.imports.ImportedIssueKeys

object ImportedIssueKeysOps extends BaseTableOps {

  def store(entity: ImportedIssueKeys): Update0 =
    sql"""
      insert into imported_issue_keys
        (src_issue_id, src_issue_index, dst_issue_id, dst_issue_index) 
      values 
        (${entity.srcIssueId}, ${entity.optSrcIssueIndex}, ${entity.dstIssueId}, ${entity.optDstIssueIndex})
    """.update

  def findLatest(): Query0[ImportedIssueKeys] =
    sql"""
      select * from imported_issue_keys order by src_issue_id desc limit 1;
    """.query[ImportedIssueKeys]

  def createTable(): Update0 =
    sql"""
      create table imported_issue_keys (
        src_issue_id    integer not null primary key,
        src_issue_index integer,
        dst_issue_id    integer not null,
        dst_issue_index integer
      )
    """.update
}
