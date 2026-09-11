package com.nulabinc.backlog.migration.common.conf

import java.nio.file.{Path, Paths}

import scala.concurrent.duration._

case class BacklogApiConfiguration(
    url: String,
    key: String,
    projectKey: String,
    backlogOutputPath: Path = Paths.get("./backlog"),
    readInterval: FiniteDuration = BacklogApiConfiguration.DefaultReadInterval,
    writeInterval: FiniteDuration = BacklogApiConfiguration.DefaultWriteInterval
) extends BacklogConfiguration {
  val isNAISpace: Boolean = url.contains(NaiSpaceDomain)
}

object BacklogApiConfiguration {

  /**
   * How long a service pauses before the reads it has always paused before: an issue, a wiki, a
   * document, a page of comments.
   *
   * Backlog allows 600 reads a minute, one every 100 ms, so half a second is five times what the
   * limit asks; it is what the services have always waited, kept so nothing changes pace unless a
   * tool asks it to.
   */
  val DefaultReadInterval: FiniteDuration = 500.millis

  /**
   * How long a service pauses before each write to Backlog.
   *
   * Backlog allows 150 updates a minute, one every 400 ms. The pause runs before the request
   * rather than between request starts, so the response time adds to it: at half a second a write
   * costs 700 to 800 ms in practice, about half of what the limit allows. Half a second is what
   * the services have always waited; a tool that has measured its own imports may pass less.
   */
  val DefaultWriteInterval: FiniteDuration = 500.millis
}
