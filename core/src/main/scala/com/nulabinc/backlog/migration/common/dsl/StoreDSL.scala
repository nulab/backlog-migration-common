package com.nulabinc.backlog.migration.common.dsl

import com.nulabinc.backlog.migration.common.domain.exports.ExportedBacklogStatus
import com.nulabinc.backlog.migration.common.domain.imports.ImportedIssueKeys
import com.nulabinc.backlog.migration.common.domain.{BacklogStatus, BacklogStatuses}
import simulacrum.typeclass

@typeclass
trait StoreDSL[F[_]] {
  def findBacklogStatus(id: Int): F[Option[BacklogStatus]]
  def storeBacklogStatus(status: BacklogStatus): F[Unit]
  def storeBacklogStatus(statuses: BacklogStatuses): F[Unit]
  def allSrcStatus: F[Seq[ExportedBacklogStatus]]
  def storeSrcStatus(status: ExportedBacklogStatus): F[Unit]
  def storeSrcStatus(statuses: Seq[ExportedBacklogStatus]): F[Unit]
  def storeImportedIssueKeys(importedIssueKeys: ImportedIssueKeys): F[Unit]
  def getLatestImportedIssueKeys(): F[ImportedIssueKeys]
}
