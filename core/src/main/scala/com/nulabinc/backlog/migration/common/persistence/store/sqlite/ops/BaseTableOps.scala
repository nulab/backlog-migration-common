package com.nulabinc.backlog.migration.common.persistence.store.sqlite.ops

import doobie.Update0

trait BaseTableOps {
  def createTable(): Update0
}
