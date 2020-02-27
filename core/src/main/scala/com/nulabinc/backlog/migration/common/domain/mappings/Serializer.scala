package com.nulabinc.backlog.migration.common.domain.mappings

trait Serializer[A, B] {
  def serialize(a: A): B
}