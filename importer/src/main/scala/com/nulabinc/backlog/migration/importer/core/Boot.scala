package com.nulabinc.backlog.migration.importer.core

import cats.Monad
import com.google.inject.Guice
import com.osinka.i18n.Messages
import com.nulabinc.backlog.migration.common.dsl.ConsoleDSL
import com.nulabinc.backlog.migration.common.conf.BacklogApiConfiguration
import com.nulabinc.backlog.migration.common.messages.ConsoleMessages
import com.nulabinc.backlog.migration.common.utils.{ConsoleOut, Logging}
import com.nulabinc.backlog.migration.importer.modules.BacklogModule
import com.nulabinc.backlog.migration.importer.service.ProjectImporter
import monix.execution.Scheduler
import monix.eval.Task

/**
 * @author uchida
 */
object Boot extends Logging {

  def execute(
      apiConfig: BacklogApiConfiguration,
      fitIssueKey: Boolean,
      retryCount: Int
  )(implicit s: Scheduler, consoleDSL: ConsoleDSL[Task]): Unit =
    try {
      val injector =
        Guice.createInjector(new BacklogModule(apiConfig))

      consoleDSL.println(ConsoleMessages.Imports.start).runAsyncAndForget

      val projectImporter = injector.getInstance(classOf[ProjectImporter])
      projectImporter.execute(fitIssueKey, retryCount)
    } catch {
      case e: Throwable =>
        consoleDSL.errorln(ConsoleMessages.cliUnknownError(e)).runAsyncAndForget
        throw e
    }

}
