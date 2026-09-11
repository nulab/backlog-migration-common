package com.nulabinc.backlog.migration.common.conf

import java.nio.file.{Path, Paths}

import scala.concurrent.duration._

case class BacklogApiConfiguration(
    url: String,
    key: String,
    projectKey: String,
    backlogOutputPath: Path = Paths.get("./backlog"),
    readInterval: FiniteDuration = BacklogApiConfiguration.DefaultReadInterval,
    writeInterval: FiniteDuration = BacklogApiConfiguration.DefaultWriteInterval,
    /** True paces from the rate-limit headers instead, with the intervals above as floors. */
    adaptiveRateLimit: Boolean = false
) extends BacklogConfiguration {
  val isNAISpace: Boolean = url.contains(NaiSpaceDomain)
}

object BacklogApiConfiguration {

  /** What the services have always waited before a read. 600 a minute would allow 100 ms. */
  val DefaultReadInterval: FiniteDuration = 500.millis

  /** What the services have always waited before a write. 150 a minute would allow 400 ms. */
  val DefaultWriteInterval: FiniteDuration = 500.millis
}
