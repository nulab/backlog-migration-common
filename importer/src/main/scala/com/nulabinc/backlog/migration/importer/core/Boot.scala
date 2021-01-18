package com.nulabinc.backlog.migration.importer.core

import cats.Monad
import cats.syntax.all._
import com.google.inject.Guice
import com.osinka.i18n.Messages
import com.nulabinc.backlog.migration.common.dsl.{ConsoleDSL, StoreDSL}
import com.nulabinc.backlog.migration.common.conf.BacklogApiConfiguration
import com.nulabinc.backlog.migration.common.messages.ConsoleMessages
import com.nulabinc.backlog.migration.common.utils.{ConsoleOut, Logging}
import com.nulabinc.backlog.migration.common.persistence.store.ReadQuery
import com.nulabinc.backlog.migration.importer.modules.BacklogModule
import com.nulabinc.backlog.migration.importer.service.ProjectImporter
import monix.execution.Scheduler
import monix.eval.Task

/**
 * @author uchida
 */
object Boot extends Logging {

  def execute[F[_]: Monad: ConsoleDSL: StoreDSL](
      apiConfig: BacklogApiConfiguration,
      fitIssueKey: Boolean,
      retryCount: Int
  )(implicit s: Scheduler): F[Either[Throwable, Unit]] =
    try {
      val injector =
        Guice.createInjector(new BacklogModule(apiConfig))
      val projectImporter = injector.getInstance(classOf[ProjectImporter[F]])

      for {
        _ <- ConsoleDSL[F].println(ConsoleMessages.Imports.start)
        _ <- projectImporter.execute(fitIssueKey, retryCount)
      } yield Right(())
    } catch {
      case e: Throwable =>
        for {
          _ <- ConsoleDSL[F].errorln(ConsoleMessages.cliUnknownError(e))
        } yield Left(e)
    }

}
