package com.nulabinc.backlog.migration.common.interpreters

import java.nio.file.Paths

import cats.effect.Blocker
import cats.effect.IO
import com.nulabinc.backlog.migration.common.domain.BacklogCustomStatus
import com.nulabinc.backlog.migration.common.domain.BacklogStatusName
import com.nulabinc.backlog.migration.common.domain.BacklogDefaultStatus
import com.nulabinc.backlog.migration.common.domain.BacklogStatuses
import com.nulabinc.backlog.migration.common.domain.Id
import com.nulabinc.backlog.migration.common.persistence.store.sqlite.ops.BacklogStatusOps
import doobie._
import doobie.implicits._
import org.scalatest._

trait TestFixture {
  implicit val cs = IO.contextShift(ExecutionContexts.synchronous)
  val dbPath      = Paths.get("./test.db")
  val transactor = Transactor.fromDriverManager[IO](
    "org.sqlite.JDBC",
    s"jdbc:sqlite:${dbPath.toAbsolutePath()}",
    "",
    "",
    Blocker.liftExecutionContext(ExecutionContexts.synchronous)
  )
  def setup(): Unit = dbPath.toFile().delete()
}

class SQLiteStoreDSLSpec
    extends funsuite.AnyFunSuite
    with matchers.must.Matchers
    with doobie.scalatest.IOChecker
    with TestFixture {

  val defaultStatus = BacklogDefaultStatus(Id.backlogStatusId(2), BacklogStatusName("Open"), 999)
  val customStatus =
    BacklogCustomStatus(Id.backlogStatusId(1), BacklogStatusName("aaa"), 123, "color")

  setup()

  test("createTable") { check(BacklogStatusOps.createTable()) }

  test("store backlog status") {
    import BacklogStatusOps._

    createTable().run.transact(transactor).unsafeRunSync()
    store(customStatus).run.transact(transactor).unsafeRunSync() mustBe 1
    store(defaultStatus).run.transact(transactor).unsafeRunSync() mustBe 1
    find(1).option.transact(transactor).unsafeRunSync() mustBe Some(customStatus)
    find(2).option.transact(transactor).unsafeRunSync() mustBe Some(defaultStatus)
  }

  test("store backlog statuses") {
    val statuses = BacklogStatuses(
      Seq(
        defaultStatus.copy(id = Id.backlogStatusId(3)),
        customStatus.copy(id = Id.backlogStatusId(4))
      )
    )
    BacklogStatusOps.store(statuses).transact(transactor).unsafeRunSync() mustBe 2
  }

}
