package com.nulabinc.backlog.migration.common.formatters

trait Formatter[A] {
  def format(value: A): (String, String)
}
