package com.nulabinc.backlog.migration.importer.core

import cats.Monad
import cats.syntax.all._
import com.google.inject.Guice
import com.nulabinc.backlog.migration.common.conf.BacklogApiConfiguration
import com.nulabinc.backlog.migration.common.dsl.{ConsoleDSL, StoreDSL}
import com.nulabinc.backlog.migration.common.messages.ConsoleMessages
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog.migration.importer.modules.BacklogModule
import com.nulabinc.backlog.migration.importer.service.ProjectImporter
import monix.eval.Task
import monix.execution.Scheduler

/**
 * @author uchida
 */
object Boot extends Logging {

  def execute(
      apiConfig: BacklogApiConfiguration,
      fitIssueKey: Boolean,
      retryCount: Int
  )(implicit
      s: Scheduler,
      storeDSL: StoreDSL[Task],
      consoleDSL: ConsoleDSL[Task]
  ): Task[Either[Throwable, Unit]] =
    try {
      val injector =
        Guice.createInjector(new BacklogModule(apiConfig))
      val projectImporter = injector.getInstance(classOf[ProjectImporter])

      for {
        _ <- ConsoleDSL[Task].println(ConsoleMessages.Imports.start)
        _ <- projectImporter.execute(fitIssueKey, retryCount)
      } yield Right(())
    } catch {
      case e: Throwable =>
        for {
          _ <- ConsoleDSL[Task].errorln(ConsoleMessages.cliUnknownError(e))
        } yield Left(e)
    }

}
