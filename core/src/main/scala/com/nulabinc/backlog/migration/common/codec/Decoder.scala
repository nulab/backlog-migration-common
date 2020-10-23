package com.nulabinc.backlog.migration.common.codec

trait Decoder[A, B] {
  def decode(a: A): B
}
