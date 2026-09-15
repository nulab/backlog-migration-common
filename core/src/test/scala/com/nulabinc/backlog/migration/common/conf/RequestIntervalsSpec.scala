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

  "the intervals" should "default to the fixed half second the services have always waited" in {
    val config = BacklogApiConfiguration("url", "key", "projectKey")
    config.readInterval should be(500.millis)
    config.writeInterval should be(500.millis)
    config.adaptiveRateLimit should be(false)
    RequestIntervals.default.adaptive should be(false)
  }

  they should "reach the services and the rate limiter through DefaultModule as configured" in {
    val config = BacklogApiConfiguration(
      "url",
      "key",
      "projectKey",
      readInterval = 100.millis,
      writeInterval = 300.millis,
      adaptiveRateLimit = true
    )
    val injector  = Guice.createInjector(new DefaultModule(config))
    val intervals = injector.getInstance(classOf[RequestIntervals])
    intervals.read should be(100.millis)
    intervals.write should be(300.millis)
    intervals.adaptive should be(true)
    injector.getInstance(classOf[BacklogRateLimiter]).floors should be theSameInstanceAs intervals
  }

  "a pause" should "wait the configured time in fixed mode" in {
    val intervals = new RequestIntervals(read = 150.millis, write = 250.millis)
    millisTaken(intervals.pauseBeforeRead()) should be >= 150L
    millisTaken(intervals.pauseBeforeWrite()) should be >= 250L
  }

  it should "do nothing in adaptive mode, where the client paces instead" in {
    val intervals = new RequestIntervals(read = 1.second, write = 1.second, adaptive = true)
    millisTaken(intervals.pauseBeforeRead()) should be < 100L
    millisTaken(intervals.pauseBeforeWrite()) should be < 100L
  }

  "the HTTP client" should "be throttled only in adaptive mode" in {
    BacklogAPIClientImpl.create(new BacklogRateLimiter(RequestIntervals.default)) should not be a[
      ThrottledBacklogHttpClient
    ]
    BacklogAPIClientImpl.create(
      new BacklogRateLimiter(new RequestIntervals(1.milli, 1.milli, adaptive = true))
    ) shouldBe a[ThrottledBacklogHttpClient]
  }

  it should "stay bare when created without a limiter" in {
    BacklogAPIClientImpl.create should not be a[ThrottledBacklogHttpClient]
  }

  private def millisTaken(f: => Unit): Long = {
    val started = System.nanoTime()
    f
    (System.nanoTime() - started) / 1000000
  }
}
