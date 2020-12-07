package com.nulabinc.backlog.migration.common.interpreters

import java.nio.file.Path
import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor
import com.nulabinc.backlog.migration.common.dsl.StoreDSL
import com.nulabinc.backlog.migration.common.domain.BacklogStatus
import com.nulabinc.backlog.migration.common.interpreters.persistence.BacklogStatusOps
import monix.eval.Task
import monix.execution.Scheduler
import com.nulabinc.backlog.migration.common.domain.BacklogStatuses

case class SQLiteStoreDSL(private val dbPath: Path)(implicit sc: Scheduler)
    extends StoreDSL[Task] {

  private val xa: Transactor[Task] = Transactor.fromDriverManager[Task](
    "org.sqlite.JDBC",
    s"jdbc:sqlite:${dbPath.toAbsolutePath()}",
    "",
    ""
  )

  private lazy val backlogStatusOps = new BacklogStatusOps

  def findBacklogStatus(id: Int): Task[Option[BacklogStatus]] =
    backlogStatusOps.find(id).option.transact(xa)

  def storeBacklogStatus(status: BacklogStatus): Task[Unit] =
    backlogStatusOps.store(status).run.transact(xa).map(_ => ())

  def storeBacklogStatus(statuses: BacklogStatuses): Task[Unit] =
    backlogStatusOps.store(statuses).transact(xa).map(_ => ())

  def createTable(): Task[Unit] =
    for {
      _ <- backlogStatusOps.createTable().run.transact(xa)
    } yield ()
}
