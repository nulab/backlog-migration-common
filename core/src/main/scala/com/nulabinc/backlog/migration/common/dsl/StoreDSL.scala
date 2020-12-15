package com.nulabinc.backlog.migration.common.dsl

import com.nulabinc.backlog.migration.common.domain.BacklogStatus
import com.nulabinc.backlog.migration.common.domain.BacklogStatuses
import simulacrum.typeclass

@typeclass
trait StoreDSL[F[_]] {
  def findBacklogStatus(id: Int): F[Option[BacklogStatus]]
  def storeBacklogStatus(status: BacklogStatus): F[Unit]
  def storeBacklogStatus(statuses: BacklogStatuses): F[Unit]
  def createTable(): F[Unit]
}
