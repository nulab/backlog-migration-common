package com.nulabinc.backlog.migration.common.domain.mappings

trait Formatter[A] {
  def status(mapping: StatusMapping[A]): (String, String)
}
