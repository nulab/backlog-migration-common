package com.nulabinc.backlog.migration.common.client

import com.nulabinc.backlog.migration.common.conf.RequestIntervals
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog4j.http.BacklogHttpResponse

/**
 * Pacing state for one API key: the last window seen per bucket and when the last request there
 * went out. A caller reserves its slot under the lock and sleeps outside it. Only installed in
 * adaptive mode; see `BacklogAPIClientImpl.create`.
 */
class BacklogRateLimiter(
    val floors: RequestIntervals,
    clock: () => Long = () => System.currentTimeMillis(),
    sleep: Long => Unit = millis => Thread.sleep(millis)
) extends Logging {

  def adaptive: Boolean = floors.adaptive

  private var windows: Map[RateLimitBucket, RateLimitWindow] = Map.empty

  private var lastRequestAt: Map[RateLimitBucket, Long] = Map.empty

  private var lastRecorded: Option[RateLimitWindow] = None

  /** Reads have their own floor; everything else shares the write floor. */
  def floorMillis(bucket: RateLimitBucket): Long =
    bucket match {
      case RateLimitBucket.Read => floors.read.toMillis
      case _                    => floors.write.toMillis
    }

  def throttle(bucket: RateLimitBucket): Unit = {
    val delay = synchronized {
      val now = clock()
      val wait = RateLimitPolicy.delayBeforeNext(
        windows.get(bucket),
        floorMillis(bucket),
        now,
        lastRequestAt.get(bucket)
      )
      lastRequestAt += bucket -> (now + wait)
      wait
    }

    if (delay >= 5000)
      logger.info(
        s"Waiting ${delay / 1000}s for the Backlog $bucket rate limit window to reset."
      )

    if (delay > 0) sleep(delay)
  }

  def record(bucket: RateLimitBucket, response: BacklogHttpResponse): Unit =
    RateLimitWindow.of(response).foreach { window =>
      synchronized {
        windows += bucket -> window
        lastRecorded = Some(window)
      }
    }

  def window(bucket: RateLimitBucket): Option[RateLimitWindow] = synchronized(windows.get(bucket))

  /** Until the reset the refusal reported; a full minute when nothing was recorded. */
  def delayAfterTooManyRequests(): Long =
    synchronized(RateLimitPolicy.delayAfterTooManyRequests(lastRecorded, clock()))
}
