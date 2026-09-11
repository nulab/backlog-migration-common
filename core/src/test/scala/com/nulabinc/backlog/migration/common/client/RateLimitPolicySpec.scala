package com.nulabinc.backlog.migration.common.client

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RateLimitPolicySpec extends AnyFlatSpec with Matchers {

  private val now      = 1700000000000L // fixed clock, so the tests do not depend on wall time
  private val nowEpoch = now / 1000

  private def window(limit: Long, remaining: Long, resetInSeconds: Long) =
    Some(RateLimitWindow(limit, remaining, nowEpoch + resetInSeconds))

  "delayBeforeNext" should "use the configured floor before anything is known" in {
    RateLimitPolicy.delayBeforeNext(None, 440L, now, Some(now)) shouldBe 440L
  }

  it should "not wait before the first request" in {
    RateLimitPolicy.delayBeforeNext(None, 440L, now, None) shouldBe 0L
    RateLimitPolicy.delayBeforeNext(window(150, 140, 60), 440L, now, None) shouldBe 0L
  }

  it should "space reads to the 600-a-minute limit with a tenth in hand" in {
    RateLimitPolicy.delayBeforeNext(window(600, 590, 60), 0L, now, Some(now)) shouldBe 110L
  }

  it should "space writes to the 150-a-minute limit with a tenth in hand" in {
    RateLimitPolicy.delayBeforeNext(window(150, 140, 60), 0L, now, Some(now)) shouldBe 440L
  }

  it should "honour a floor the operator has raised" in {
    RateLimitPolicy.delayBeforeNext(window(600, 590, 60), 5000L, now, Some(now)) shouldBe 5000L
  }

  it should "count the previous request's round trip toward the gap" in {
    val w = window(150, 140, 60) // 440 ms apart
    RateLimitPolicy.delayBeforeNext(w, 0L, now, Some(now - 300L)) shouldBe 140L
    RateLimitPolicy.delayBeforeNext(w, 0L, now, Some(now - 440L)) shouldBe 0L
    RateLimitPolicy.delayBeforeNext(w, 0L, now, Some(now - 5000L)) shouldBe 0L
  }

  it should "wait for the window to reset when the allowance is nearly gone" in {
    // 150-limit window with 10 left is under the 10% low-water mark.
    RateLimitPolicy.delayBeforeNext(window(150, 10, 30), 0L, now, Some(now)) shouldBe 31000L
  }

  it should "call the allowance nearly gone at a tenth of the limit, not one above" in {
    RateLimitPolicy.delayBeforeNext(window(150, 15, 30), 0L, now, Some(now)) shouldBe 31000L
    RateLimitPolicy.delayBeforeNext(window(150, 16, 30), 0L, now, Some(now)) shouldBe 440L
  }

  it should "keep one request in reserve when a tenth of the limit rounds to nothing" in {
    RateLimitPolicy.delayBeforeNext(window(5, 1, 30), 0L, now, Some(now)) shouldBe 31000L
    RateLimitPolicy.delayBeforeNext(window(5, 2, 30), 0L, now, Some(now)) should be < 31000L
  }

  it should "wait for the reset however long ago the last request went out" in {
    RateLimitPolicy.delayBeforeNext(window(150, 10, 30), 0L, now, Some(now - 5000L)) shouldBe
    31000L
  }

  it should "never wait longer than a window and a half, however odd the reset time" in {
    RateLimitPolicy.delayBeforeNext(window(150, 0, 86400), 0L, now, Some(now)) shouldBe 90000L
  }

  it should "not wait on a reset time that has already passed" in {
    RateLimitPolicy.delayBeforeNext(window(150, 0, -120), 440L, now, Some(now)) shouldBe 440L
  }

  it should "treat a limit of zero as no information" in {
    RateLimitPolicy.delayBeforeNext(window(0, 0, 30), 440L, now, Some(now)) shouldBe 440L
  }

  "delayAfterTooManyRequests" should "wait until the window resets" in {
    RateLimitPolicy.delayAfterTooManyRequests(window(150, 0, 42), now) shouldBe 43000L
  }

  it should "fall back to a full minute when the header told us nothing" in {
    RateLimitPolicy.delayAfterTooManyRequests(None, now) shouldBe 60000L
  }

  // Clocks disagree: a reset already in the past says nothing useful either.
  it should "fall back to a full minute when the reset time has already passed" in {
    RateLimitPolicy.delayAfterTooManyRequests(window(150, 0, -120), now) shouldBe 60000L
  }

  "spacingFor" should "give the gap that fits a minute's allowance, plus a tenth" in {
    RateLimitPolicy.spacingFor(600) shouldBe 110L
    RateLimitPolicy.spacingFor(150) shouldBe 440L
    RateLimitPolicy.spacingFor(60) shouldBe 1100L
    RateLimitPolicy.spacingFor(0) shouldBe 0L
  }
}
