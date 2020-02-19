package com.nulabinc.backlog.migration.common.service

import java.nio.file.Path

import cats.Monad
import cats.implicits._
import com.nulabinc.backlog.migration.common.domain.BacklogStatuses
import com.nulabinc.backlog.migration.common.dsl.{ConsoleDSL, StorageDSL}

trait StatusMappingFileService[A] {
  import com.nulabinc.backlog.migration.common.messages.ConsoleMessages.Mappings

  def init[F[_]: Monad: StorageDSL: ConsoleDSL](path: Path, srcItems: Seq[A], dstItems: BacklogStatuses): F[Unit] = {
    for {
      exists <- StorageDSL[F].exists(path)
      _ <- if (exists)
        // displayMergedMappingFileMessageToConsole
        ConsoleDSL[F].println("") // TODO
      else {
        // displayCreateMappingFileMessageToConsole
        for {
          _ <- StorageDSL[F].writeFile(path, "") // TODO
          _ <- ConsoleDSL[F].println(Mappings.statusMappingCreated(path))
        } yield ()
      }
    } yield ()
  }

}

