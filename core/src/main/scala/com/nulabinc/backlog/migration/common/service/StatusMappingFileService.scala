package com.nulabinc.backlog.migration.common.service

import java.nio.file.Path

import cats.Monad
import cats.implicits._
import com.nulabinc.backlog.migration.common.deserializers.Deserializer
import com.nulabinc.backlog.migration.common.domain.BacklogStatuses
import com.nulabinc.backlog.migration.common.domain.mappings._
import com.nulabinc.backlog.migration.common.dsl.{ConsoleDSL, StorageDSL}
import com.nulabinc.backlog.migration.common.formatters.Formatter
import com.nulabinc.backlog.migration.common.serializers.Serializer
import org.apache.commons.csv.CSVRecord

private case class MergedStatusMapping[A](mergeList: Seq[StatusMapping[A]], addedList: Seq[StatusMapping[A]])

private object MergedStatusMapping {
  def empty[A]: MergedStatusMapping[A] = MergedStatusMapping[A](mergeList = Seq(), addedList = Seq())
}

object StatusMappingFileService {
  import com.nulabinc.backlog.migration.common.messages.ConsoleMessages.{Mappings => MappingMessages}

  def init[A, F[_]: Monad: StorageDSL: ConsoleDSL](mappingFilePath: Path,
                                                   mappingListPath: Path,
                                                   srcItems: Seq[A],
                                                   dstItems: BacklogStatuses)
                                                  (implicit formatter: Formatter[StatusMapping[A]],
                                                   serializer: Serializer[StatusMapping[A], Seq[String]],
                                                   deserializer: Deserializer[CSVRecord, StatusMapping[A]],
                                                   header: MappingHeader[StatusMapping[_]]): F[Unit] =
    for {
      exists <- StorageDSL[F].exists(mappingFilePath)
      _ <- if (exists) {
        for {
          records <- StorageDSL[F].read(mappingFilePath, MappingFileService.readLine)
          mappings = MappingDeserializer.status(records)
          result = merge(mappings, srcItems)
          _ <- if (result.addedList.nonEmpty)
            for {
              _ <- StorageDSL[F].writeNewFile(mappingFilePath, MappingSerializer.status(result.mergeList))
              _ <- ConsoleDSL[F].println(MappingMessages.statusMappingMerged(mappingFilePath, result.addedList))
            } yield ()
          else
            ConsoleDSL[F].println(MappingMessages.statusMappingNoChanges)
        } yield ()
      } else {
        val result = merge(Seq(), srcItems)
        for {
          _ <- StorageDSL[F].writeNewFile(mappingFilePath, MappingSerializer.status(result.mergeList))
          _ <- ConsoleDSL[F].println(MappingMessages.statusMappingCreated(mappingFilePath))
        } yield ()
      }
      _ <- StorageDSL[F].writeNewFile(mappingListPath, MappingSerializer.statusList(dstItems))
    } yield ()

  private def merge[A](mappings: Seq[StatusMapping[A]], srcItems: Seq[A]): MergedStatusMapping[A] =
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

