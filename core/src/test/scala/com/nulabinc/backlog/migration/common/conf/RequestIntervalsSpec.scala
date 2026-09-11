package com.nulabinc.backlog.migration.common.conf

import com.google.inject.Guice
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

  private def millisTaken(f: => Unit): Long = {
    val started = System.nanoTime()
    f
    (System.nanoTime() - started) / 1000000
  }
}
