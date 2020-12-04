package com.nulabinc.backlog.migration.common.interpreters

import java.nio.file.Path
import doobie._
import doobie.implicits._
import com.nulabinc.backlog.migration.common.dsl.StoreDSL
import monix.eval.Task
import monix.execution.Scheduler
import com.nulabinc.backlog.migration.common.domain.BacklogStatus
import doobie.util.transactor.Transactor
import com.nulabinc.backlog.migration.common.interpreters.persistence.BacklogStatusOps

case class SQLiteStoreDSL(private val dbPath: Path)(implicit sc: Scheduler)
    extends StoreDSL[Task] {

  private val xa: Transactor[Task] = Transactor.fromDriverManager[Task](
    "org.sqlite.JDBC",
    s"jdbc:sqlite:${dbPath.toAbsolutePath()}",
    "",
    ""
  )

  private lazy val backlogStatusOps = new BacklogStatusOps(xa)

  def storeBacklogStatus(status: BacklogStatus): Task[Unit] =
    backlogStatusOps.storeBacklogStatus(status)

  def createTable(): Task[Unit] =
    for {
      _ <- backlogStatusOps.createTable()
    } yield ()
}
