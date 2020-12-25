package com.nulabinc.backlog.migration.common.dsl

import com.nulabinc.backlog.migration.common.domain.BacklogStatus
import com.nulabinc.backlog.migration.common.domain.BacklogStatuses
import com.nulabinc.backlog.migration.common.domain.exports.ExportedBacklogStatus
import com.nulabinc.backlog.migration.common.persistence.store.{ReadQuery, WriteQuery}
import simulacrum.typeclass

@typeclass
trait StoreDSL[F[_]] {
  def findBacklogStatus(id: Int): F[Option[BacklogStatus]]
  def storeBacklogStatus(status: BacklogStatus): F[Unit]
  def storeBacklogStatus(statuses: BacklogStatuses): F[Unit]
  def allSrcStatus: F[Seq[ExportedBacklogStatus]]
  def storeSrcStatus(status: ExportedBacklogStatus): F[Unit]
  def storeSrcStatus(statuses: Seq[ExportedBacklogStatus]): F[Unit]
}
