package com.nulabinc.backlog.migration.common.domain.mappings

trait Deserializer[A, B] {
  def deserialize(a: A): B
}
