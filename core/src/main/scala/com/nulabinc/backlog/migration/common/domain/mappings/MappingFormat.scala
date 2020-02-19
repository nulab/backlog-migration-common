package com.nulabinc.backlog.migration.common.domain.mappings

trait MappingFormat[A] {
  def formatStatus(mapping: StatusMapping[A]): (String, String)
}
