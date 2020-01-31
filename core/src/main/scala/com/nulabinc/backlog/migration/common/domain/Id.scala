package com.nulabinc.backlog.migration.common.domain

import com.nulabinc.backlog.migration.common.domain.Types.AnyId

case class Id[T] private(value: AnyId) extends AnyVal

object Id {

}