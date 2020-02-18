package com.nulabinc.backlog.migration.common.persistence.sqlite.ops

import monix.execution.Scheduler
import slick.jdbc.SQLiteProfile.api._

case class AllTableOps()(implicit exc: Scheduler) {
  val backlogStatusTableOps = StatusTableOps()

  val createDatabaseOps =
    DBIO.seq(
      backlogStatusTableOps.createTable
    )
}
