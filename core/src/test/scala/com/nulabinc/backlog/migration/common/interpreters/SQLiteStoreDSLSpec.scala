package com.nulabinc.backlog.migration.common.interpreters

import com.nulabinc.backlog.migration.common.domain._
import com.nulabinc.backlog.migration.common.domain.exports.{
  DeletedExportedBacklogStatus,
  ExistingExportedBacklogStatus
}
import monix.execution.Scheduler
import org.scalatest._

import java.nio.file.Paths
import com.nulabinc.backlog.migration.common.domain.imports.ImportedIssueKeys

trait TestFixture {
  implicit val sc: Scheduler = monix.execution.Scheduler.global
  private val dbPath         = Paths.get("./test.db")

  val dsl = new SQLiteStoreDSL(dbPath)

  def setup(): Unit = {
    dbPath.toFile.delete()
    dsl.createTable.runSyncUnsafe()
  }
}

class SQLiteStoreDSLSpec
    extends funsuite.AnyFunSuite
    with matchers.must.Matchers
    with TestFixture {

  val defaultStatus = BacklogDefaultStatus(Id.backlogStatusId(2), BacklogStatusName("Open"), 999)
  val customStatus =
    BacklogCustomStatus(Id.backlogStatusId(1), BacklogStatusName("aaa"), 123, "color")

  test("store backlog status") {
    setup()

    dsl.storeBacklogStatus(customStatus).runSyncUnsafe()
    dsl.storeBacklogStatus(defaultStatus).runSyncUnsafe()
    dsl.findBacklogStatus(1).runSyncUnsafe() mustBe Some(customStatus)
    dsl.findBacklogStatus(2).runSyncUnsafe() mustBe Some(defaultStatus)
  }

  test("store backlog statuses") {
    setup()

    val statuses = BacklogStatuses(
      Seq(
        defaultStatus.copy(id = Id.backlogStatusId(3)),
        customStatus.copy(id = Id.backlogStatusId(4))
      )
    )
    dsl.storeBacklogStatus(statuses).runSyncUnsafe() mustBe ()
  }

  val existing         = ExistingExportedBacklogStatus(customStatus)
  val deleted          = DeletedExportedBacklogStatus(BacklogStatusName("bbb"))
  val exportedStatuses = Seq(existing, deleted)

  test("store exported status") {
    setup()

    // insert
    dsl.storeSrcStatus(existing).runSyncUnsafe()
    dsl.storeSrcStatus(deleted).runSyncUnsafe()
    dsl.allSrcStatus.runSyncUnsafe() mustBe exportedStatuses

    // update
    val updated =
      ExistingExportedBacklogStatus(customStatus.copy(name = BacklogStatusName("updated status")))
    dsl.storeSrcStatus(updated).runSyncUnsafe()
    dsl.allSrcStatus.runSyncUnsafe() mustBe Seq(updated, deleted)
  }

  test("store exported statuses") {
    setup()

    dsl.storeSrcStatus(exportedStatuses).runSyncUnsafe()
    dsl.allSrcStatus.runSyncUnsafe().length mustBe 2
  }

  val imported1 = ImportedIssueKeys(1111, 1, 100, 100)
  val imported2 = ImportedIssueKeys(2222, 2, 101, 101)

  test("store imported issue keys") {
    setup()

    dsl.storeImportedIssueKeys(imported1).runSyncUnsafe() mustBe ()
    dsl.storeImportedIssueKeys(imported2).runSyncUnsafe() mustBe ()

    dsl.getLatestImportedIssueKeys().runSyncUnsafe() mustBe imported2
  }
}
