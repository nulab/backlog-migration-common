package com.nulabinc.backlog.migration.common.dsl

import com.nulabinc.backlog.migration.common.domain.BacklogStatus

trait StoreDSL[F[_]] {
  def findBacklogStatus(id: Int): F[Option[BacklogStatus]]
  def storeBacklogStatus(status: BacklogStatus): F[Unit]
  def createTable(): F[Unit]
}
