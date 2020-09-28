package com.nulabinc.backlog.migration.common.domain.mappings

sealed trait MappingType

object MappingType {
  case object User     extends MappingType
  case object Status   extends MappingType
  case object Priority extends MappingType
}
