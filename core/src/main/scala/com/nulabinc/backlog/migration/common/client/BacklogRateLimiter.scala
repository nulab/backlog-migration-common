package com.nulabinc.backlog.migration.common.client

import com.nulabinc.backlog.migration.common.conf.RequestIntervals
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog4j.http.BacklogHttpResponse

/**
 * Pacing state for one API key: the last window seen per bucket and when the last request there
 * went out. A caller reserves its slot under the lock and sleeps outside it. Only installed in
 * adaptive mode; see `BacklogAPIClientImpl.create`.
 *
 * `clock` is UNIX time in milliseconds and is only compared against the reset timestamps Backlog
 * reports. `ticker` is a monotonic millisecond counter used for the spacing between requests, so a
 * wall-clock adjustment neither stalls the limiter nor lets requests bunch up.
 */
class BacklogRateLimiter(
    val floors: RequestIntervals,
    clock: () => Long = () => System.currentTimeMillis(),
    ticker: () => Long = () => System.nanoTime() / 1000000L,
    sleep: Long => Unit = millis => Thread.sleep(millis)
) extends Logging {

  def adaptive: Boolean = floors.adaptive

  private var windows: Map[RateLimitBucket, RateLimitWindow] = Map.empty

  /** Ticker time at which the last request in each bucket was allowed to go out. */
  private var lastSentAt: Map[RateLimitBucket, Long] = Map.empty

  /** The window each bucket's most recent 429 carried; `None` when it carried no headers. */
  private var refusals: Map[RateLimitBucket, Option[RateLimitWindow]] = Map.empty

  /** Reads have their own floor; everything else shares the write floor. */
  def floorMillis(bucket: RateLimitBucket): Long =
    bucket match {
      case RateLimitBucket.Read => floors.read.toMillis
      case _                    => floors.write.toMillis
    }

  def throttle(bucket: RateLimitBucket): Unit = {
    val delay = synchronized {
      val now  = clock()
      val tick = ticker()
      val wait = RateLimitPolicy.delayBeforeNext(
        windows.get(bucket),
        floorMillis(bucket),
        now,
        lastSentAt.get(bucket).map(tick - _)
      )
      lastSentAt += bucket -> (tick + wait)
      wait
    }

    if (delay >= 5000)
      logger.info(
        s"Pacing Backlog $bucket requests: waiting ${delay / 1000}s before the next one."
      )

    if (delay > 0) sleep(delay)
  }

  /**
   * Responses can complete out of send order, so an older window never replaces a newer one. A
   * refusal is also remembered on its own so the retry for that bucket waits on the window the
   * refusal itself reported.
   */
  def record(bucket: RateLimitBucket, response: BacklogHttpResponse): Unit = {
    val observed = RateLimitWindow.of(response)
    synchronized {
      observed.foreach { window =>
        windows += bucket -> RateLimitPolicy.latest(windows.get(bucket), window)
      }
      if (response.getStatusCode == BacklogRateLimiter.TooManyRequests)
        refusals += bucket -> observed
    }
  }

  def window(bucket: RateLimitBucket): Option[RateLimitWindow] = synchronized(windows.get(bucket))

  /**
   * Until the reset the bucket's last refusal reported; a full minute when that refusal carried no
   * headers or nothing was refused in this bucket, as in fixed mode.
   */
  def delayAfterTooManyRequests(bucket: RateLimitBucket): Long =
    synchronized(
      RateLimitPolicy.delayAfterTooManyRequests(refusals.get(bucket).flatten, clock())
    )
}

object BacklogRateLimiter {
  val TooManyRequests = 429
}
