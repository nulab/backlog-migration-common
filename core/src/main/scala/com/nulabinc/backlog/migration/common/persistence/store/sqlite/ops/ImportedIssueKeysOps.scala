package com.nulabinc.backlog.migration.common.persistence.store.sqlite.ops

import com.nulabinc.backlog.migration.common.domain.imports.ImportedIssueKeys
import doobie._
import doobie.implicits._

object ImportedIssueKeysOps extends BaseTableOps {

  def store(entity: ImportedIssueKeys): Update0 =
    sql"""
      insert into imported_issue_keys
        (src_issue_id, src_issue_index, dst_issue_id, dst_issue_index) 
      values
        (${entity.srcIssueId}, ${entity.srcIssueIndex}, ${entity.dstIssueId}, ${entity.dstIssueIndex})
    """.update

  def findLatest(): Query0[ImportedIssueKeys] =
    sql"""
      select * from imported_issue_keys order by dst_issue_id desc limit 1;
    """.query[ImportedIssueKeys]

  def findBySrcIssueIdLatest(id: Long): Query0[ImportedIssueKeys] =
    sql"""
      select * from imported_issue_keys where src_issue_id = ${id} order by src_issue_id desc limit 1;
    """.query[ImportedIssueKeys]

  def createTable(): Update0 =
    sql"""
      create table imported_issue_keys (
        src_issue_id    integer not null,
        src_issue_index integer not null,
        dst_issue_id    integer not null primary key,
        dst_issue_index integer not null
      )
    """.update
}
