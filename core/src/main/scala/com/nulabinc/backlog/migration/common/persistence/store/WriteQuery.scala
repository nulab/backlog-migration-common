package com.nulabinc.backlog.migration.common.persistence.store

import doobie.util.update.Update0

trait WriteQuery[A] {
  def write(entity: A): Update0
}
