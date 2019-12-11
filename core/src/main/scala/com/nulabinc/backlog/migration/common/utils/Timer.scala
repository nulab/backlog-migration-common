package com.nulabinc.backlog.migration.common.utils

/**
  * @author uchida
  */
object Timer {
  var s: Long = 0L
  def time(label: String) = {
    ConsoleOut.println(label + ":" + (System.currentTimeMillis - s))
    s = System.currentTimeMillis
  }
}
