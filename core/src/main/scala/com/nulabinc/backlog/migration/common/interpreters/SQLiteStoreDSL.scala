package com.nulabinc.backlog.migration.common.interpreters

import cats.implicits._
import com.nulabinc.backlog.migration.common.domain.exports.ExportedBacklogStatus
import com.nulabinc.backlog.migration.common.domain.{BacklogStatus, BacklogStatuses}
import com.nulabinc.backlog.migration.common.dsl.StoreDSL
import com.nulabinc.backlog.migration.common.persistence.store.sqlite.ops.{
  BacklogStatusOps,
  ExportedStatusTableOps
}
import doobie.implicits._
import doobie.util.transactor.Transactor
import monix.eval.Task
import monix.execution.Scheduler

import java.nio.file.Path

class SQLiteStoreDSL(private val dbPath: Path)(implicit sc: Scheduler) extends StoreDSL[Task] {

  private val xa: Transactor[Task] = Transactor.fromDriverManager[Task](
    "org.sqlite.JDBC",
    s"jdbc:sqlite:${dbPath.toAbsolutePath}",
    "",
    ""
  )

  def findBacklogStatus(id: Int): Task[Option[BacklogStatus]] =
    BacklogStatusOps.find(id).option.transact(xa)

  def allBacklogStatuses(): Task[BacklogStatuses] =
    Task
      .from(
        BacklogStatusOps.getAll().to[Seq].transact(xa)
      )
      .map(BacklogStatuses)

  def storeBacklogStatus(status: BacklogStatus): Task[Unit] =
    BacklogStatusOps.store(status).run.transact(xa).map(_ => ())

  def storeBacklogStatus(statuses: BacklogStatuses): Task[Unit] =
    BacklogStatusOps.store(statuses).transact(xa).map(_ => ())

  def allSrcStatus: Task[Seq[ExportedBacklogStatus]] =
    Task.from(
      ExportedStatusTableOps.getAll.to[Seq].transact(xa)
    )

  def storeSrcStatus(status: ExportedBacklogStatus): Task[Unit] =
    ExportedStatusTableOps.store(status).run.transact(xa).map(_ => ())

  override def storeSrcStatus(statuses: Seq[ExportedBacklogStatus]): Task[Unit] =
    Task.from(
      ExportedStatusTableOps.store(statuses).transact(xa).map(_ => ())
    )

  def createTable: Task[Unit] =
    (
      BacklogStatusOps.createTable().run,
      ExportedStatusTableOps.createTable().run
    ).mapN(_ + _).transact(xa).map(_ => ())

}
