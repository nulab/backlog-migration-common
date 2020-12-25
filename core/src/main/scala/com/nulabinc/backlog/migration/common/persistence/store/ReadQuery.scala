package com.nulabinc.backlog.migration.common.persistence.store

import doobie.util.query.Query0

trait ReadQuery[A] {
  def read(): Query0[A]
}
