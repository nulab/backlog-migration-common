package com.nulabinc.backlog.migration.common.interpreters.persistence

import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor
import cats.effect.Async
import cats.Monad
import cats.Monad.ops._
import com.nulabinc.backlog.migration.common.domain.BacklogStatus
import com.nulabinc.backlog.migration.common.domain.BacklogDefaultStatus
import com.nulabinc.backlog.migration.common.domain.BacklogCustomStatus

trait BaseTableOps[F[_]] {
  val tableName: String
  def createTable(): F[Unit]
}

class BacklogStatusOps[F[_]: Monad: Async](xa: Transactor[F]) extends BaseTableOps[F] {

  val tableName: String = "backlog_statuses"

  def storeBacklogStatus(status: BacklogStatus): F[Unit] =
    sql"""
      insert into $tableName 
        (id, name, display_order, color, is_custom) 
      values 
        (${status.id}, ${status.name.trimmed}, ${status.displayOrder}, ${status.optColor}, ${status.isCustomStatus})
      on conflict(id) 
      do update set 
        name = ${status.name.trimmed},
        display_order = ${status.displayOrder},
        color = ${status.optColor},
        is_custom = ${status.isCustomStatus}
    """.update.run.transact(xa).map(_ => ())

  override def createTable(): F[Unit] =
    sql"""
      create table $tableName (
        id              bigint primary key,
        name            text   not null,
        display_order   int    not null,
        color           text,
        is_custom       int    not null
      )
    """.update.run.transact(xa).map(_ => ())
}
