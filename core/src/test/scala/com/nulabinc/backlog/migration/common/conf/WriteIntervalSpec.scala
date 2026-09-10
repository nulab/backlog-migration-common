package com.nulabinc.backlog.migration.common.conf

import com.google.inject.Guice
import com.nulabinc.backlog.migration.common.modules.DefaultModule
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

class WriteIntervalSpec extends AnyFlatSpec with Matchers {

  "the interval" should "default to the half second the services always waited" in {
    BacklogApiConfiguration("url", "key", "projectKey").writeInterval should be(500.millis)
  }

  it should "reach the services through DefaultModule as configured" in {
    val config   = BacklogApiConfiguration("url", "key", "projectKey", writeInterval = 300.millis)
    val injector = Guice.createInjector(new DefaultModule(config))
    injector.getInstance(classOf[WriteInterval]).duration should be(300.millis)
  }

  "pause" should "wait the configured time" in {
    val started = System.nanoTime()
    new WriteInterval(200.millis).pause()
    (System.nanoTime() - started) / 1000000 should be >= 200L
  }
}
