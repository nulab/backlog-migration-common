package com.nulabinc.backlog.migration.common.conf

import scala.concurrent.duration.FiniteDuration

/**
 * The pauses a service takes before a request to Backlog.
 *
 * The services have always slept half a second before a write, and before a few reads; these are
 * those sleeps with their length taken from `BacklogApiConfiguration`, which `DefaultModule` binds
 * this from. Where a service pauses, and before what, is unchanged.
 */
class RequestIntervals(val read: FiniteDuration, val write: FiniteDuration) {

  def pauseBeforeRead(): Unit = Thread.sleep(read.toMillis)

  def pauseBeforeWrite(): Unit = Thread.sleep(write.toMillis)

}
