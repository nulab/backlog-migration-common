package com.nulabinc.backlog.migration.common.conf

import java.io.File

import com.nulabinc.backlog.migration.common.client.IAAH
import com.typesafe.config.{ConfigException, ConfigFactory}

/**
 * @author uchida
 */
trait BacklogConfiguration {

  val NaiSpaceDomain = "backlog.com"

  val internal = ConfigFactory.load()

  val external = ConfigFactory.parseFile(new File("./application.conf"))

  val applicationName = internal.getString("application.title")

  val versionName = internal.getString("application.version")

  val language = internal.getString("application.language")

  val productName = internal.getString("application.product")

  val productVersion = internal.getString("application.version")

  val backlog4jVersion = internal.getString("application.backlog4jVersion")

  val exportLimitAtOnce = {
    try {
      external.getInt("application.export-limit-at-once")
    } catch {
      case _: ConfigException =>
        internal.getInt("application.export-limit-at-once")
    }
  }

  val akkaMailBoxPool = {
    try {
      external.getInt("application.akka.mailbox-pool")
    } catch {
      case _: ConfigException =>
        internal.getInt("application.akka.mailbox-pool")
    }
  }

  def getBacklogConfiguration() = {
    try {
      external.getInt("application.akka.mailbox-pool")
      external
    } catch {
      case _: ConfigException =>
        internal
    }
  }

}
