package com.nulabinc.backlog.migration.common.interpreters

import java.nio.file.Path

import com.nulabinc.backlog.migration.common.domain.{BacklogStatus, BacklogStatuses}
import com.nulabinc.backlog.migration.common.dsl.StoreDSL
import com.nulabinc.backlog.migration.common.interpreters.persistence.BacklogStatusOps
import com.nulabinc.backlog.migration.common.persistence.store.ReadQuery
import doobie.implicits._
import doobie.util.transactor.Transactor
import monix.eval.Task
import monix.execution.Scheduler
import doobie.util.query.Query0

abstract class SQLiteStoreDSL(private val dbPath: Path)(implicit sc: Scheduler)
    extends StoreDSL[Task] {

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

  def allSrcStatus[A]()(implicit query: ReadQuery[A]): Task[Seq[A]] =
    Task.from(
      query.read().to[Seq].transact(xa)
    )

  def createTable(): Task[Unit] =
    for {
      _ <- BacklogStatusOps.createTable().run.transact(xa)
    } yield ()
}
