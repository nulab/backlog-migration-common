package com.nulabinc.backlog.migration.common.codec

trait Encoder[A, B] {
  def encode(a: A): B
}
