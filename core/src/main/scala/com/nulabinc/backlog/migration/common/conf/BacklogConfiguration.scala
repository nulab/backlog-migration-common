package com.nulabinc.backlog.migration.common.conf

import java.io.File

import com.nulabinc.backlog.migration.common.client.IAAH
import com.typesafe.config.{Config, ConfigException, ConfigFactory}

/**
 * @author
 *   uchida
 */
trait BacklogConfiguration {

  val NaiSpaceDomain = "backlog.com"

  val internal: Config = ConfigFactory.load()

  val external: Config = ConfigFactory.parseFile(new File("./application.conf"))

  val applicationName: String = internal.getString("application.title")

  val versionName: String = internal.getString("application.version")

  val language: String = internal.getString("application.language")

  val productName: String = internal.getString("application.product")

  val productVersion: String = internal.getString("application.version")

  val backlog4jVersion: String = internal.getString("application.backlog4jVersion")

  val defaultRetryCount = internal.getInt("application.defaultRetryCount")

  val iaah: IAAH = IAAH(internal.getString("application.iaah"))

  val exportLimitAtOnce: Int = {
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
