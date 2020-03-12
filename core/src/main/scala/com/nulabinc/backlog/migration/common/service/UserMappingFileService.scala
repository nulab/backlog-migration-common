package com.nulabinc.backlog.migration.common.service

import java.nio.file.Path

import cats.Monad
import cats.implicits._
import com.nulabinc.backlog.migration.common.domain.BacklogUser
import com.nulabinc.backlog.migration.common.domain.mappings._
import com.nulabinc.backlog.migration.common.dsl.{ConsoleDSL, StorageDSL}
import org.apache.commons.csv.CSVRecord

private case class MergedUserMapping[A](mergeList: Seq[UserMapping[A]], addedList: Seq[UserMapping[A]])

private object MergedUserMapping {
  def empty[A]: MergedUserMapping[A] = MergedUserMapping[A](mergeList = Seq(), addedList = Seq())
}

object UserMappingFileService {
  import com.nulabinc.backlog.migration.common.messages.ConsoleMessages.{Mappings => MappingMessages}

  def init[A, F[_]: Monad: StorageDSL: ConsoleDSL](path: Path, srcItems: Seq[A], dstItems: Seq[BacklogUser])
                                                  (implicit formatter: Formatter[UserMapping[A]],
                                                   serializer: Serializer[UserMapping[A], Seq[String]],
                                                   deserializer: Deserializer[CSVRecord, UserMapping[A]],
                                                   mappingHeader: MappingHeader[UserMapping[_]]): F[Unit] = {
    val header = MappingSerializer.fromHeader(mappingHeader)

    for {
      exists <- StorageDSL[F].exists(path)
      _ <- if (exists) {
        for {
          records <- StorageDSL[F].read(path, MappingFileService.readLine)
          mappings = MappingDeserializer.user(records)
          result = merge(mappings, srcItems)
          _ <- if (result.addedList.nonEmpty)
            for {
              _ <- StorageDSL[F].writeNewFile(path, header +: MappingSerializer.user(result.mergeList))
              _ <- ConsoleDSL[F].println(MappingMessages.userMappingMerged(path, result.addedList))
            } yield ()
          else
            ConsoleDSL[F].println(MappingMessages.userMappingNoChanges)
        } yield ()
      } else {
        val result = merge(Seq(), srcItems)
        for {
          _ <- StorageDSL[F].writeNewFile(path, header +: MappingSerializer.user(result.mergeList))
          _ <- ConsoleDSL[F].println(MappingMessages.userMappingCreated(path))
        } yield ()
      }
    } yield ()
  }

  private def merge[A](mappings: Seq[UserMapping[A]], srcItems: Seq[A]): MergedUserMapping[A] =
    srcItems.foldLeft(MergedUserMapping.empty[A]) { (acc, item) =>
      mappings.find(_.src == item) match {
        case Some(value) =>
          acc.copy(mergeList = acc.mergeList :+ value)
        case None =>
          val mapping = UserMapping.create(item)
          acc.copy(mergeList = acc.mergeList :+ mapping, addedList = acc.addedList :+ mapping)
      }
    }

}

