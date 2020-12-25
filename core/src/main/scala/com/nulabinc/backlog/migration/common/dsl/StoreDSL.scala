package com.nulabinc.backlog.migration.common.dsl

import com.nulabinc.backlog.migration.common.domain.BacklogStatus
import com.nulabinc.backlog.migration.common.domain.BacklogStatuses
import com.nulabinc.backlog.migration.common.persistence.store.{ReadQuery, WriteQuery}
import simulacrum.typeclass

@typeclass
trait StoreDSL[F[_]] {
  def findBacklogStatus(id: Int): F[Option[BacklogStatus]]
  def storeBacklogStatus(status: BacklogStatus): F[Unit]
  def storeBacklogStatus(statuses: BacklogStatuses): F[Unit]
  def allSrcStatus[A]()(implicit query: ReadQuery[A]): F[Seq[A]]
  def storeSrcStaus[A](entity: A)(implicit query: WriteQuery[A]): F[Unit]
  def createTable(): F[Unit]
}
