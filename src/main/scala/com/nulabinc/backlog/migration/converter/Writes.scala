package com.nulabinc.backlog.migration.converter

trait Writes[A, B] {

  def writes(a: A): B

}
