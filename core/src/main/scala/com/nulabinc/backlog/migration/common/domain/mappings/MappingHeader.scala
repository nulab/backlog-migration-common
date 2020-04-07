package com.nulabinc.backlog.migration.common.domain.mappings

trait MappingHeader[+A <: Mapping[_]] {
  val headers: Seq[String]
}
