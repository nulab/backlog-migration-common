package com.nulabinc.backlog.migration.importer.core

import com.google.inject.Guice
import com.nulabinc.backlog.migration.common.conf.BacklogApiConfiguration
import com.nulabinc.backlog.migration.common.utils.{ConsoleOut, Logging}
import com.nulabinc.backlog.migration.importer.modules.BacklogModule
import com.nulabinc.backlog.migration.importer.service.ProjectImporter
import com.osinka.i18n.Messages

/**
  * @author uchida
  */
object Boot extends Logging {

  def execute(
      apiConfig: BacklogApiConfiguration,
      fitIssueKey: Boolean,
      retryCount: Int
  ): Unit =
    try {
      val injector = Guice.createInjector(new BacklogModule(apiConfig))
      ConsoleOut.println(
        s"""
                            |${Messages("import.start")}
                            |--------------------------------------------------""".stripMargin
      )

      val projectImporter = injector.getInstance(classOf[ProjectImporter])
      projectImporter.execute(fitIssueKey, retryCount)
    } catch {
      case e: Throwable =>
        ConsoleOut.error(s"${Messages("cli.error.unknown")}:${e.getMessage}")
        throw e
    }

}
