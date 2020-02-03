package com.nulabinc.backlog.migration.common.domain.mappings

import com.nulabinc.backlog.migration.common.domain.Entity
import com.nulabinc.backlog.migration.common.domain.Types.AnyId

sealed trait Mapping

trait StatusMapping[A] extends Mapping with Entity {
  val id: AnyId
  val optSrc: Option[A]
  val optDst: Option[BacklogStatusMappingItem]
}

case class BacklogStatusMappingItem(value: MappingValue)