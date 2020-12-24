package com.nulabinc.backlog.migration.common.interpreters

import java.nio.file.Path

import com.nulabinc.backlog.migration.common.domain.{BacklogStatus, BacklogStatuses}
import com.nulabinc.backlog.migration.common.dsl.StoreDSL
import com.nulabinc.backlog.migration.common.interpreters.persistence.BacklogStatusOps
import doobie.implicits._
import doobie.util.transactor.Transactor
import monix.eval.Task
import monix.execution.Scheduler

abstract class SQLiteStoreDSL(private val dbPath: Path)(implicit sc: Scheduler)
    extends StoreDSL[Task] {

  private val xa: Transactor[Task] = Transactor.fromDriverManager[Task](
    "org.sqlite.JDBC",
    s"jdbc:sqlite:${dbPath.toAbsolutePath}",
    "",
    ""
  )

  protected lazy val backlogStatusOps = new BacklogStatusOps

  def findBacklogStatus(id: Int): Task[Option[BacklogStatus]] =
    backlogStatusOps.find(id).option.transact(xa)

  def allBacklogStatuses(): Task[BacklogStatuses] =
    Task
      .from(
        backlogStatusOps.getAll().to[Seq].transact(xa)
      )
      .map(BacklogStatuses)

  def storeBacklogStatus(status: BacklogStatus): Task[Unit] =
    backlogStatusOps.store(status).run.transact(xa).map(_ => ())

  def storeBacklogStatus(statuses: BacklogStatuses): Task[Unit] =
    backlogStatusOps.store(statuses).transact(xa).map(_ => ())

  def createTable(): Task[Unit] =
    for {
      _ <- backlogStatusOps.createTable().run.transact(xa)
    } yield ()
}
