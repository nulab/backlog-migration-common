package com.nulabinc.backlog.migration.common.domain.mappings

trait StatusMapping[A] {
  val optSrc: Option[A]
  val optDst: Option[BacklogStatusMappingItem]
}

case class BacklogStatusMappingItem(value: MappingValue)