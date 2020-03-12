package com.nulabinc.backlog.migration.common.service

import java.nio.file.Path

import cats.Monad
import cats.implicits._
import com.nulabinc.backlog.migration.common.domain.mappings._
import com.nulabinc.backlog.migration.common.dsl.{ConsoleDSL, StorageDSL}
import com.nulabinc.backlog4j.Priority
import org.apache.commons.csv.CSVRecord

private case class MergedPriorityMapping[A](mergeList: Seq[PriorityMapping[A]], addedList: Seq[PriorityMapping[A]])

private object MergedPriorityMapping {
  def empty[A]: MergedPriorityMapping[A] = MergedPriorityMapping[A](mergeList = Seq(), addedList = Seq())
}

object PriorityMappingFileService {
  import com.nulabinc.backlog.migration.common.messages.ConsoleMessages.{Mappings => MappingMessages}

  def init[A, F[_]: Monad: StorageDSL: ConsoleDSL](path: Path, srcItems: Seq[A], dstItems: Seq[Priority])
                                                  (implicit formatter: Formatter[PriorityMapping[A]],
                                                   serializer: Serializer[PriorityMapping[A], Seq[String]],
                                                   deserializer: Deserializer[CSVRecord, PriorityMapping[A]],
                                                   mappingHeader: MappingHeader[PriorityMapping[_]]): F[Unit] = {
    val header = MappingSerializer.fromHeader(mappingHeader)

    for {
      exists <- StorageDSL[F].exists(path)
      _ <- if (exists) {
        for {
          records <- StorageDSL[F].read(path, MappingFileService.readLine)
          mappings = MappingDeserializer.priority(records)
          result = merge(mappings, srcItems)
          _ <- if (result.addedList.nonEmpty)
            for {
              _ <- StorageDSL[F].writeNewFile(path, header +: MappingSerializer.priority(result.mergeList))
              _ <- ConsoleDSL[F].println(MappingMessages.priorityMappingMerged(path, result.addedList))
            } yield ()
          else
            ConsoleDSL[F].println(MappingMessages.priorityMappingNoChanges)
        } yield ()
      } else {
        val result = merge(Seq(), srcItems)
        for {
          _ <- StorageDSL[F].writeNewFile(path, header +: MappingSerializer.priority(result.mergeList))
          _ <- ConsoleDSL[F].println(MappingMessages.priorityMappingCreated(path))
        } yield ()
      }
    } yield ()
  }

  private def merge[A](mappings: Seq[PriorityMapping[A]], srcItems: Seq[A]): MergedPriorityMapping[A] =
    srcItems.foldLeft(MergedPriorityMapping.empty[A]) { (acc, item) =>
      mappings.find(_.src == item) match {
        case Some(value) =>
          acc.copy(mergeList = acc.mergeList :+ value)
        case None =>
          val mapping = PriorityMapping.create(item)
          acc.copy(mergeList = acc.mergeList :+ mapping, addedList = acc.addedList :+ mapping)
      }
    }

}

