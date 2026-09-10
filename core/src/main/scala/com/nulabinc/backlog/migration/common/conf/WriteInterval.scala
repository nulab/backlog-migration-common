package com.nulabinc.backlog.migration.common.conf

import scala.concurrent.duration.FiniteDuration

/**
 * The pause a service takes before each write to Backlog.
 *
 * The services have always slept half a second before a write; this is that sleep with its length
 * taken from `BacklogApiConfiguration.writeInterval`, which `DefaultModule` binds it from. Where a
 * service pauses, and before what, is unchanged.
 */
class WriteInterval(val duration: FiniteDuration) {

  def pause(): Unit = Thread.sleep(duration.toMillis)

}
