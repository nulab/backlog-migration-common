package com.nulabinc.backlog.migration.convert

trait Writes[A, B] {

  def writes(a: A): B

}
