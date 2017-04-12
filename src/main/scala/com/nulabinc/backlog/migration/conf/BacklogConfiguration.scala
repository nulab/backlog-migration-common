package com.nulabinc.backlog.migration.conf

import java.io.File

import com.typesafe.config.{ConfigException, ConfigFactory}

/**
  * @author uchida
  */
trait BacklogConfiguration {

  val internal = ConfigFactory.load()

  val external = ConfigFactory.parseFile(new File("./application.conf"))

  val applicationName = internal.getString("application.title")

  val mixpanelToken = internal.getString("application.mixpanel.token")

  val mixpanelProduct = internal.getString("application.mixpanel.product")

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
      external.getInt("backlog.migration.akka.mailbox-pool")
    } catch {
      case _: ConfigException =>
        internal.getInt("backlog.migration.akka.mailbox-pool")
    }
  }

  def getBacklogConfiguration() = {
    try {
      external.getInt("backlog.migration.akka.mailbox-pool")
      external
    } catch {
      case _: ConfigException =>
        internal
    }
  }

}
