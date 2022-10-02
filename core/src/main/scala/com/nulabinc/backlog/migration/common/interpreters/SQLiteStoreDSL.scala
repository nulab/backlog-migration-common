package com.nulabinc.backlog.migration.common.interpreters

import java.nio.file.Path

import cats.implicits._
import com.nulabinc.backlog.migration.common.domain.exports.ExportedBacklogStatus
import com.nulabinc.backlog.migration.common.domain.imports.ImportedIssueKeys
import com.nulabinc.backlog.migration.common.domain.{BacklogStatus, BacklogStatuses}
import com.nulabinc.backlog.migration.common.dsl.StoreDSL
import com.nulabinc.backlog.migration.common.persistence.store.sqlite.ops.{
  BacklogStatusOps,
  ExportedStatusTableOps,
  ImportedIssueKeysOps
}
import doobie.implicits._
import doobie.util.transactor.Transactor
import monix.eval.Task

class SQLiteStoreDSL(private val dbPath: Path) extends StoreDSL[Task] {

  protected val xa: Transactor[Task] = Transactor.fromDriverManager[Task](
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
    ExportedStatusTableOps.store(status).transact(xa).map(_ => ())

  def storeSrcStatus(statuses: Seq[ExportedBacklogStatus]): Task[Unit] =
    Task.from(
      ExportedStatusTableOps.store(statuses).transact(xa).map(_ => ())
    )

  def storeImportedIssueKeys(importedIssueKeys: ImportedIssueKeys): Task[Unit] =
    ImportedIssueKeysOps.store(importedIssueKeys).run.transact(xa).map(_ => ())

  def getLatestImportedIssueKeys(): Task[ImportedIssueKeys] =
    ImportedIssueKeysOps.findLatest().option.transact(xa).map(_.getOrElse(ImportedIssueKeys.empty))

  def findBySrcIssueIdLatest(srcIssueId: Long): Task[Option[ImportedIssueKeys]] =
    ImportedIssueKeysOps.findBySrcIssueIdLatest(srcIssueId).option.transact(xa)

  def createTable: Task[Unit] =
    (
      BacklogStatusOps.createTable().run,
      ExportedStatusTableOps.createTable().run,
      ImportedIssueKeysOps.createTable().run
    ).mapN(_ + _ + _).transact(xa).map(_ => ())

}

object SQLiteStoreDSL {
  def apply(dbPath: Path): SQLiteStoreDSL =
    new SQLiteStoreDSL(dbPath)
}
