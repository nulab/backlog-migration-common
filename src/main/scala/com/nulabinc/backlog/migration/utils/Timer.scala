package com.nulabinc.backlog.migration.utils

/**
  * @author uchida
  */
object Timer {
  var s: Long = 0L
  def time(label: String) = {
    ConsoleOut.info(label + ":" + (System.currentTimeMillis - s))
    s = System.currentTimeMillis
  }
}
