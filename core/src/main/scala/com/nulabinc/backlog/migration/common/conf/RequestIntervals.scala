package com.nulabinc.backlog.migration.common.conf

import scala.concurrent.duration.FiniteDuration

/**
 * The pauses a service takes before a request to Backlog. In adaptive mode the client paces itself
 * from the rate-limit headers instead and these do nothing.
 */
class RequestIntervals(
    val read: FiniteDuration,
    val write: FiniteDuration,
    val adaptive: Boolean = false
) {

  def pauseBeforeRead(): Unit = if (!adaptive) Thread.sleep(read.toMillis)

  def pauseBeforeWrite(): Unit = if (!adaptive) Thread.sleep(write.toMillis)

}

object RequestIntervals {

  val default: RequestIntervals =
    new RequestIntervals(
      BacklogApiConfiguration.DefaultReadInterval,
      BacklogApiConfiguration.DefaultWriteInterval
    )
}
