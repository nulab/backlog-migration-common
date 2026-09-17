package com.nulabinc.backlog.migration.common.client

import com.nulabinc.backlog.migration.common.conf.{BacklogApiConfiguration, RequestIntervals}
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog4j.BacklogAPIException
import com.nulabinc.backlog4j.http.BacklogHttpResponse

/**
 * Pacing state for one API key: the last window seen per bucket, when the last request there went
 * out, and how many requests are still in flight. A caller reserves its slot under the lock and
 * sleeps outside it; the reservation counts against the bucket's remaining allowance until the
 * response is recorded (or abandoned), so concurrent callers cannot all pass the low-water mark on
 * the same stale count. Only installed in adaptive mode; see `BacklogAPIClientImpl.create`.
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

  private var windows: Map[RateLimitBucket, RateLimitWindow] = Map.empty

  /** Ticker time at which the last request in each bucket was allowed to go out. */
  private var lastSentAt: Map[RateLimitBucket, Long] = Map.empty

  /** Requests `throttle` has let through whose response has not been recorded or abandoned yet. */
  private var inFlight: Map[RateLimitBucket, Int] = Map.empty

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
        lastSentAt.get(bucket).map(tick - _),
        inFlight.getOrElse(bucket, 0)
      )
      lastSentAt += bucket -> (tick + wait)
      inFlight += bucket   -> (inFlight.getOrElse(bucket, 0) + 1)
      wait
    }

    if (delay >= 5000)
      logger.info(
        s"Pacing Backlog $bucket requests: waiting ${delay / 1000}s before the next one."
      )

    if (delay > 0) sleep(delay)
  }

  /**
   * Every response with headers, refusals included, updates the bucket's window used for pacing.
   * Responses can complete out of send order, so an older window never replaces a newer one. What
   * is not kept here is which window belonged to which refusal: that travels with the
   * `TooManyRequestsException` for the refused request, so the retry waits on its own reset and
   * concurrent refusals in one bucket cannot overwrite each other. Recording also settles the
   * reservation `throttle` made for this request.
   */
  def record(bucket: RateLimitBucket, response: BacklogHttpResponse): Unit = {
    val observed = RateLimitWindow.of(response)
    synchronized {
      settle(bucket)
      observed.foreach { window =>
        windows += bucket -> RateLimitPolicy.latest(windows.get(bucket), window)
      }
    }
  }

  /** The request never got a response, so the slot `throttle` reserved for it is free again. */
  def abandon(bucket: RateLimitBucket): Unit = synchronized(settle(bucket))

  private def settle(bucket: RateLimitBucket): Unit =
    inFlight += bucket -> math.max(0, inFlight.getOrElse(bucket, 0) - 1)

  def inFlightCount(bucket: RateLimitBucket): Int = synchronized(inFlight.getOrElse(bucket, 0))

  def window(bucket: RateLimitBucket): Option[RateLimitWindow] = synchronized(windows.get(bucket))

  /**
   * Until the reset this refusal reported; a full minute when it carried no headers, or when it is
   * a plain backlog4j 429 from the bare client in fixed mode.
   */
  def delayAfterTooManyRequests(refusal: BacklogAPIException): Long = {
    val window = refusal match {
      case refused: TooManyRequestsException => refused.window
      case _                                 => None
    }
    RateLimitPolicy.delayAfterTooManyRequests(window, clock())
  }
}

object BacklogRateLimiter {
  val TooManyRequests = 429

  /** A limiter only when the configuration opts in; its intervals become the pacing floors. */
  def of(apiConfig: BacklogApiConfiguration): Option[BacklogRateLimiter] =
    if (apiConfig.adaptiveRateLimit)
      Some(
        new BacklogRateLimiter(
          new RequestIntervals(apiConfig.readInterval, apiConfig.writeInterval)
        )
      )
    else None
}
