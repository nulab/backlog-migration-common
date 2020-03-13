package com.nulabinc.backlog.migration.common.service

import java.nio.file.Path

import cats.Monad
import cats.implicits._
import com.nulabinc.backlog.migration.common.conf.BacklogApiConfiguration
import com.nulabinc.backlog.migration.common.deserializers.Deserializer
import com.nulabinc.backlog.migration.common.domain.BacklogUser
import com.nulabinc.backlog.migration.common.domain.mappings._
import com.nulabinc.backlog.migration.common.dsl.{ConsoleDSL, StorageDSL}
import com.nulabinc.backlog.migration.common.formatters.Formatter
import com.nulabinc.backlog.migration.common.serializers.Serializer
import org.apache.commons.csv.CSVRecord

private case class MergedUserMapping[A](mergeList: Seq[UserMapping[A]], addedList: Seq[UserMapping[A]])

private object MergedUserMapping {
  def empty[A]: MergedUserMapping[A] = MergedUserMapping[A](mergeList = Seq(), addedList = Seq())
}

object UserMappingFileService {
  import com.nulabinc.backlog.migration.common.messages.ConsoleMessages.{Mappings => MappingMessages}

  def init[A, F[_]: Monad: StorageDSL: ConsoleDSL](mappingFilePath: Path,
                                                   mappingListPath: Path,
                                                   srcItems: Seq[A],
                                                   dstItems: Seq[BacklogUser],
                                                   dstApiConfiguration: BacklogApiConfiguration)
                                                  (implicit formatter: Formatter[UserMapping[A]],
                                                   serializer: Serializer[UserMapping[A], Seq[String]],
                                                   deserializer: Deserializer[CSVRecord, UserMapping[A]],
                                                   mappingHeader: MappingHeader[UserMapping[_]]): F[Unit] = {
    val header = MappingSerializer.fromHeader(mappingHeader)

    for {
      mappingFileExists <- StorageDSL[F].exists(mappingFilePath)
      _ <- if (mappingFileExists) {
        for {
          recordsWithHeader <- StorageDSL[F].read(mappingFilePath, MappingFileService.readLine)
          records = recordsWithHeader.tail
          mappings = MappingDeserializer.user(records)
          result = merge(mappings, srcItems, dstApiConfiguration.isNAISpace)
          _ <- if (result.addedList.nonEmpty)
            for {
              _ <- StorageDSL[F].writeNewFile(mappingFilePath, header +: MappingSerializer.user(result.mergeList))
              _ <- ConsoleDSL[F].println(MappingMessages.userMappingMerged(mappingFilePath, result.addedList))
            } yield ()
          else
            ConsoleDSL[F].println(MappingMessages.userMappingNoChanges)
        } yield ()
      } else {
        val result = merge(Seq(), srcItems, dstApiConfiguration.isNAISpace)
        for {
          _ <- StorageDSL[F].writeNewFile(mappingFilePath, header +: MappingSerializer.user(result.mergeList))
          _ <- ConsoleDSL[F].println(MappingMessages.userMappingCreated(mappingFilePath))
        } yield ()
      }
      _ <- StorageDSL[F].writeNewFile(mappingListPath, ???)
    } yield ()
  }

  private def merge[A](mappings: Seq[UserMapping[A]], srcItems: Seq[A], isNAISpace: Boolean): MergedUserMapping[A] =
    srcItems.foldLeft(MergedUserMapping.empty[A]) { (acc, item) =>
      mappings.find(_.src == item) match {
        case Some(value) =>
          acc.copy(mergeList = acc.mergeList :+ value)
        case None =>
          val mapping = UserMapping.create(item, isNAISpace)
          acc.copy(mergeList = acc.mergeList :+ mapping, addedList = acc.addedList :+ mapping)
      }
    }

}

