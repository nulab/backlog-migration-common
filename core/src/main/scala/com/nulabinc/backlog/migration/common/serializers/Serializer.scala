package com.nulabinc.backlog.migration.common.serializers

trait Serializer[A, B] {
  def serialize(a: A): B
}