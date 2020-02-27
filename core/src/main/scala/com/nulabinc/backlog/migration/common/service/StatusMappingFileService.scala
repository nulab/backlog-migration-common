package com.nulabinc.backlog.migration.common.service

import java.io.InputStream
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.Path

import cats.Monad
import cats.implicits._
import com.nulabinc.backlog.migration.common.domain.BacklogStatuses
import com.nulabinc.backlog.migration.common.domain.mappings._
import com.nulabinc.backlog.migration.common.dsl.{ConsoleDSL, StorageDSL}
import org.apache.commons.csv.{CSVFormat, CSVParser, CSVRecord}

import scala.jdk.CollectionConverters._

private case class MergedStatusMapping[A](mergeList: Seq[StatusMapping[A]], addedList: Seq[StatusMapping[A]])

private object MergedStatusMapping {
  def empty[A]: MergedStatusMapping[A] = MergedStatusMapping[A](mergeList = Seq(), addedList = Seq())
}

object StatusMappingFileService {
  import com.nulabinc.backlog.migration.common.messages.ConsoleMessages.{Mappings => MappingMessages}

  private val charset: Charset = StandardCharsets.UTF_8
  private val csvFormat: CSVFormat = CSVFormat.DEFAULT.withIgnoreEmptyLines().withSkipHeaderRecord()

  def init[A, F[_]: Monad: StorageDSL: ConsoleDSL](path: Path, srcItems: Seq[A], dstItems: BacklogStatuses)
                                                  (implicit formatter: Formatter[StatusMapping[A]],
                                                   serializer: Serializer[StatusMapping[A], Seq[String]],
                                                   deserializer: Deserializer[CSVRecord, StatusMapping[A]]): F[Unit] =
    for {
      exists <- StorageDSL[F].exists(path)
      _ <- if (exists) {
        for {
          records <- StorageDSL[F].read(path, readLine)
          mappings = MappingDeserializer.status(records)
          result = merge(mappings, srcItems)
          _ <- if (result.addedList.nonEmpty)
            for {
              _ <- StorageDSL[F].writeNewFile(path, MappingSerializer.status(result.mergeList))
              _ <- ConsoleDSL[F].println(MappingMessages.statusMappingMerged(path, result.addedList))
            } yield ()
          else
            ConsoleDSL[F].println(MappingMessages.statusMappingNoChanges)
        } yield ()
      } else {
        val result = merge(Seq(), srcItems)
        for {
          _ <- StorageDSL[F].writeNewFile(path, MappingSerializer.status(result.mergeList))
          _ <- ConsoleDSL[F].println(MappingMessages.statusMappingCreated(path))
        } yield ()
      }
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

  private def readLine(is: InputStream): IndexedSeq[CSVRecord] =
    CSVParser.parse(is, charset, csvFormat)
      .getRecords.asScala
      .foldLeft(IndexedSeq.empty[CSVRecord])( (acc, item) => acc :+ item)

//  private def readCSVFile(is: InputStream): HashMap[String, String] = {
//    val parser = CSVParser.parse(is, charset, Config.csvFormat)
//    parser.getRecords.asScala.foldLeft(HashMap.empty[String, String]) {
//      case (acc, record) =>
//        acc + (record.get(0) -> record.get(1))
//    }
//  }

}

