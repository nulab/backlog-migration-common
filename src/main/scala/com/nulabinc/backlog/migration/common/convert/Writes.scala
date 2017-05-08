package com.nulabinc.backlog.migration.common.convert

trait Writes[A, B] {

  def writes(a: A): B

}
