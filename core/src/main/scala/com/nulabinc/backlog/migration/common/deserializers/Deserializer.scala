package com.nulabinc.backlog.migration.common.deserializers

trait Deserializer[A, B] {
  def deserialize(a: A): B
}
