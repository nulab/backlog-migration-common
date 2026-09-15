package com.nulabinc.backlog.migration.common.client

import com.nulabinc.backlog4j.http.BacklogHttpResponse

/**
 * Backlog counts read, update, search and icon requests separately, and the `X-RateLimit-*`
 * headers describe only the bucket the request fell in.
 */
sealed trait RateLimitBucket

object RateLimitBucket {
  case object Read   extends RateLimitBucket
  case object Update extends RateLimitBucket
  case object Search extends RateLimitBucket
  case object Icon   extends RateLimitBucket

  private val SearchPaths = Set("issues", "issues/count", "wikis", "wikis/count")

  private val IconPath = "^(space/image|projects/[^/]+/image|(users|groups|teams)/[^/]+/icon)$".r

  def of(method: String, endpoint: String): RateLimitBucket =
    if (method.equalsIgnoreCase("GET")) {
      val p = path(endpoint)
      if (SearchPaths.contains(p)) Search
      else if (IconPath.matches(p)) Icon
      else Read
    } else Update

  /** The part after `/api/v2/`, without query string or trailing slash. */
  private def path(endpoint: String): String = {
    val withoutQuery = endpoint.takeWhile(_ != '?')
    val afterHost    = withoutQuery.replaceFirst("^[A-Za-z][A-Za-z0-9+.-]*://[^/]*", "")
    afterHost.replaceFirst("^/api/v2(/|$)", "").stripPrefix("/").stripSuffix("/")
  }
}

/** `resetAt` is UNIX time in seconds. */
case class RateLimitWindow(limit: Long, remaining: Long, resetAt: Long)

object RateLimitWindow {

  /** None when the response carried no headers; backlog4j reports a missing limit as zero. */
  def of(response: BacklogHttpResponse): Option[RateLimitWindow] = {
    val limit = response.getRateLimitLimit.toLong
    Option(response.getRateLimitReset)
      .flatMap(_.trim.toLongOption)
      .filter(_ => limit > 0)
      .map(reset => RateLimitWindow(limit, response.getRateLimitRemaining.toLong, reset))
  }
}

/** Pure functions of the last window and the clock, so they can be tested without waiting. */
object RateLimitPolicy {

  private val WindowMillis = 60000L

  /** Eleven tenths of the even spacing, kept integral so 600 a minute gives exactly 110 ms. */
  private val SafetyNumerator   = 11L
  private val SafetyDenominator = 10L

  /** Below this share of the allowance, wait for the window to reset. */
  private val LowWaterMark = 0.1

  private val MaxWaitMillis = 90000L

  /**
   * Spacing is measured from when the previous request went out, so its round trip counts toward
   * the gap. `nowMillis` is UNIX time for the reset comparison; the elapsed time comes from a
   * monotonic source.
   */
  def delayBeforeNext(
      window: Option[RateLimitWindow],
      floorMillis: Long,
      nowMillis: Long,
      elapsedSinceLastMillis: Option[Long]
  ): Long = {
    val floor     = math.max(0L, floorMillis)
    val spacing   = window.map(w => math.max(spacingFor(w.limit), floor)).getOrElse(floor)
    val sinceLast = elapsedSinceLastMillis.map(e => math.max(0L, spacing - e)).getOrElse(0L)

    window match {
      case Some(w) if isLow(w) => math.max(millisUntil(w.resetAt, nowMillis), sinceLast)
      case _                   => sinceLast
    }
  }

  /** Until the window resets; a full minute when the reset time is missing or already past. */
  def delayAfterTooManyRequests(window: Option[RateLimitWindow], nowMillis: Long): Long =
    window.map(w => millisUntil(w.resetAt, nowMillis)).filter(_ > 0).getOrElse(WindowMillis)

  /**
   * The window to keep when another response arrives: a newer reset wins, an older one is ignored,
   * and within the same window the lower remaining count stands.
   */
  def latest(current: Option[RateLimitWindow], observed: RateLimitWindow): RateLimitWindow =
    current match {
      case Some(c) if observed.resetAt < c.resetAt => c
      case Some(c) if observed.resetAt == c.resetAt =>
        observed.copy(remaining = math.min(c.remaining, observed.remaining))
      case _ => observed
    }

  def spacingFor(limit: Long): Long =
    if (limit <= 0) 0L
    else {
      val numerator   = WindowMillis * SafetyNumerator
      val denominator = limit * SafetyDenominator
      (numerator + denominator - 1) / denominator
    }

  private def isLow(window: RateLimitWindow): Boolean =
    window.limit > 0 && window.remaining <= math.max(1L, (window.limit * LowWaterMark).toLong)

  private def millisUntil(epochSeconds: Long, nowMillis: Long): Long = {
    val wait = (epochSeconds * 1000L) - nowMillis + 1000L // a second's grace for clock skew
    math.min(math.max(wait, 0L), MaxWaitMillis)
  }
}
