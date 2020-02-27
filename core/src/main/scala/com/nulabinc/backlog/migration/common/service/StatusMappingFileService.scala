package com.nulabinc.backlog.migration.common.service

import java.nio.file.Path

import cats.Monad
import cats.implicits._
import com.nulabinc.backlog.migration.common.domain.BacklogStatuses
import com.nulabinc.backlog.migration.common.domain.mappings.{Formatter, MappingSerializer, Serializer, StatusMapping}
import com.nulabinc.backlog.migration.common.dsl.{ConsoleDSL, StorageDSL}

trait StatusMappingFileService[A] {
  import com.nulabinc.backlog.migration.common.messages.ConsoleMessages.{Mappings => MappingMessages}

  implicit val formatter: Formatter[A]
  implicit val serializer: Serializer[StatusMapping[A], Seq[String]]

  def init[F[_]: Monad: StorageDSL: ConsoleDSL](path: Path, mappings: Seq[StatusMapping[A]], srcItems: Seq[A], dstItems: BacklogStatuses): F[Unit] = {
    for {
      exists <- StorageDSL[F].exists(path)
      result = merge(mappings, srcItems)
      _ <- if (exists) {
        if (result.addedList.nonEmpty)
          ConsoleDSL[F].println(MappingMessages.statusMappingMerged(path, result.addedList))
        else
          ConsoleDSL[F].println(MappingMessages.statusMappingNoChanges)
      } else {
        // displayCreateMappingFileMessageToConsole
        for {
          _ <- StorageDSL[F].writeNewFile(path, MappingSerializer.status(result.mergeList))
          _ <- ConsoleDSL[F].println(MappingMessages.statusMappingCreated(path))
        } yield ()
      }
    } yield ()
  }

  case class MergedStatusMapping[A](mergeList: Seq[StatusMapping[A]], addedList: Seq[StatusMapping[A]])

  object MergedStatusMapping {
    def empty[A]: MergedStatusMapping[A] = MergedStatusMapping[A](mergeList = Seq(), addedList = Seq())
  }

  private def merge(mappings: Seq[StatusMapping[A]], srcItems: Seq[A]): MergedStatusMapping[A] =
    srcItems.foldLeft(MergedStatusMapping.empty[A]) { (acc, item) =>
      mappings.find(_.src == item) match {
        case Some(value) =>
          acc.copy(mergeList = acc.mergeList :+ value)
        case None =>
          val mapping = StatusMapping.create(item)
          acc.copy(mergeList = acc.mergeList :+ mapping, addedList = acc.addedList :+ mapping)
      }
    }

}

