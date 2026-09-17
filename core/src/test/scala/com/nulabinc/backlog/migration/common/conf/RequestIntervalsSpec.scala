package com.nulabinc.backlog.migration.common.conf

import com.google.inject.Guice
import com.nulabinc.backlog.migration.common.client.{
  BacklogAPIClientImpl,
  BacklogRateLimiter,
  ThrottledBacklogHttpClient
}
import com.nulabinc.backlog.migration.common.modules.DefaultModule
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

class RequestIntervalsSpec extends AnyFlatSpec with Matchers {

  "the intervals" should "default to the half second the services always waited" in {
    val config = BacklogApiConfiguration("url", "key", "projectKey")
    config.readInterval should be(500.millis)
    config.writeInterval should be(500.millis)
  }

  they should "reach the services through DefaultModule as configured" in {
    val config = BacklogApiConfiguration(
      "url",
      "key",
      "projectKey",
      readInterval = 100.millis,
      writeInterval = 300.millis
    )
    val intervals =
      Guice.createInjector(new DefaultModule(config)).getInstance(classOf[RequestIntervals])
    intervals.read should be(100.millis)
    intervals.write should be(300.millis)
  }

  "a pause" should "wait the configured time" in {
    val intervals = new RequestIntervals(read = 150.millis, write = 250.millis)
    millisTaken(intervals.pauseBeforeRead()) should be >= 150L
    millisTaken(intervals.pauseBeforeWrite()) should be >= 250L
  }

  "adaptive mode" should "be off by default, leaving the services and the client as they were" in {
    val config = BacklogApiConfiguration("url", "key", "projectKey")
    config.adaptiveRateLimit should be(false)
    BacklogRateLimiter.of(config) should be(None)
    BacklogAPIClientImpl.create(None) should not be a[ThrottledBacklogHttpClient]
  }

  it should "turn the service pauses off and hand pacing to the client, floored by the intervals" in {
    val config = BacklogApiConfiguration(
      "url",
      "key",
      "projectKey",
      readInterval = 100.millis,
      writeInterval = 300.millis,
      adaptiveRateLimit = true
    )
    val intervals =
      Guice.createInjector(new DefaultModule(config)).getInstance(classOf[RequestIntervals])
    intervals.read should be(Duration.Zero)
    intervals.write should be(Duration.Zero)

    val limiter = BacklogRateLimiter.of(config)
    limiter.map(_.floors.read) should be(Some(100.millis))
    limiter.map(_.floors.write) should be(Some(300.millis))
    BacklogAPIClientImpl.create(limiter) shouldBe a[ThrottledBacklogHttpClient]
  }

  private def millisTaken(f: => Unit): Long = {
    val started = System.nanoTime()
    f
    (System.nanoTime() - started) / 1000000
  }
}
