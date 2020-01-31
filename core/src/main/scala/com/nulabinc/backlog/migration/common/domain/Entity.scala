package com.nulabinc.backlog.migration.common.domain

import com.nulabinc.backlog.migration.common.domain.Types.AnyId

trait Entity {
  def id: AnyId
}
