package com.nulabinc.backlog.migration.common.services

import java.nio.file.Path

import cats.Foldable.ops._
import cats.Monad
import cats.Monad.ops._
import cats.data.Validated.{Invalid, Valid}
import com.nulabinc.backlog.migration.common.codec.StatusMappingEncoder
import com.nulabinc.backlog.migration.common.deserializers.Deserializer
import com.nulabinc.backlog.migration.common.domain.BacklogStatuses
import com.nulabinc.backlog.migration.common.domain.mappings._
import com.nulabinc.backlog.migration.common.dsl.{ConsoleDSL, StorageDSL}
import com.nulabinc.backlog.migration.common.errors.{
  MappingFileError,
  MappingFileNotFound,
  MappingValidationError,
  ValidationError
}
import com.nulabinc.backlog.migration.common.formatters.Formatter
import com.nulabinc.backlog.migration.common.validators.MappingValidatorNec
import org.apache.commons.csv.CSVRecord

private case class MergedStatusMapping[A](
    mergeList: Seq[StatusMapping[A]],
    addedList: Seq[StatusMapping[A]]
)

private object MergedStatusMapping {
  def empty[A]: MergedStatusMapping[A] =
    MergedStatusMapping[A](mergeList = Seq(), addedList = Seq())
}

object StatusMappingFileService {
  import com.nulabinc.backlog.migration.common.messages.ConsoleMessages.{
    Mappings => MappingMessages
  }
  import com.nulabinc.backlog.migration.common.shared.syntax._

  /**
    Create mapping files.
      - statuses.csv       Link the source and destination states. User must edit to link it.
      - statuses_list.csv  List of items that can be specified in statuses.csv
    */
  def init[A, F[_]: Monad: StorageDSL: ConsoleDSL](
      mappingFilePath: Path,
      mappingListPath: Path,
      srcItems: Seq[A],
      dstItems: BacklogStatuses
  )(implicit
      formatter: Formatter[StatusMapping[A]],
      encoder: StatusMappingEncoder[A],
      deserializer: Deserializer[CSVRecord, StatusMapping[A]],
      header: MappingHeader[StatusMapping[_]]
  ): F[Unit] =
    for {
      exists <- StorageDSL[F].exists(mappingFilePath)
      _ <-
        if (exists) {
          for {
            records <- StorageDSL[F].read(mappingFilePath, MappingFileService.readLine)
            mappings = MappingDeserializer.status(records)
            result   = merge(mappings, srcItems)
            _ <-
              if (result.addedList.nonEmpty)
                for {
                  _ <- StorageDSL[F].writeNewFile(
                    mappingFilePath,
                    MappingSerializer.status(result.mergeList)
                  )
                  _ <- ConsoleDSL[F].println(
                    MappingMessages.statusMappingMerged(mappingFilePath, result.addedList)
                  )
                } yield ()
              else
                ConsoleDSL[F].println(MappingMessages.statusMappingNoChanges)
          } yield ()
        } else {
          val result = merge(Seq(), srcItems)
          for {
            _ <- StorageDSL[F].writeNewFile(
              mappingFilePath,
              MappingSerializer.status(result.mergeList)
            )
            _ <- ConsoleDSL[F].println(
              MappingMessages.statusMappingCreated(mappingFilePath)
            )
          } yield ()
        }
      _ <- StorageDSL[F].writeNewFile(
        mappingListPath,
        MappingSerializer.statusList(dstItems)
      )
    } yield ()

  /**
    * List of items that can be specified in statuses.csv
    *
   * @param path
    * @param dstItems
    * @param deserializer
    * @tparam A
    * @tparam F
    * @return
    */
  def execute[A, F[_]: Monad: StorageDSL: ConsoleDSL](
      path: Path,
      dstItems: BacklogStatuses
  )(implicit
      deserializer: Deserializer[CSVRecord, StatusMapping[A]]
  ): F[Either[MappingFileError, Seq[ValidatedStatusMapping[A]]]] = {
    val result = for {
      _           <- StorageDSL[F].exists(path).orError(MappingFileNotFound("status", path)).handleError
      unvalidated <- getMappings(path).handleError
      validated   <- validateMappings(unvalidated, dstItems).lift.handleError
    } yield validated

    result.value
  }

  /**
    * Deserialize a mapping file.
    *
   * @param path
    * @param deserializer
    * @tparam A
    * @tparam F
    * @return
    */
  def getMappings[A, F[_]: Monad: ConsoleDSL: StorageDSL](path: Path)(implicit
      deserializer: Deserializer[CSVRecord, StatusMapping[A]]
  ): F[Either[MappingFileError, Seq[StatusMapping[A]]]] =
    for {
      records <- StorageDSL[F].read(path, MappingFileService.readLine)
      mappings = MappingDeserializer.status(records)
    } yield Right(mappings)

  /**
    * Validate mappings
    * @param mappings
    * @param dstItems
    * @tparam A
    * @return
    */
  def validateMappings[A](
      mappings: Seq[StatusMapping[A]],
      dstItems: BacklogStatuses
  ): Either[MappingFileError, Seq[ValidatedStatusMapping[A]]] = {
    val results = mappings
      .map(MappingValidatorNec.validateStatusMapping(_, dstItems))
      .foldLeft(ValidationResults.empty[A]) { (acc, item) =>
        item match {
          case Valid(value)   => acc.copy(values = acc.values :+ value)
          case Invalid(error) => acc.copy(errors = acc.errors ++ error.toList)
        }
      }

    results.toResult
  }

  /**
    * Merge old mappings and new items.
    *
   * @param mappings
    * @param srcItems
    * @tparam A
    * @return
    */
  private def merge[A](
      mappings: Seq[StatusMapping[A]],
      srcItems: Seq[A]
  ): MergedStatusMapping[A] =
    srcItems.foldLeft(MergedStatusMapping.empty[A]) { (acc, item) =>
      mappings.find(_.src == item) match {
        case Some(value) =>
          acc.copy(mergeList = acc.mergeList :+ value)
        case None =>
          val mapping = StatusMapping.create(item)
          acc.copy(
            mergeList = acc.mergeList :+ mapping,
            addedList = acc.addedList :+ mapping
          )
      }
    }

  private case class ValidationResults[A](
      values: Seq[ValidatedStatusMapping[A]] = Seq(),
      errors: List[ValidationError] = List()
  ) {
    def toResult: Either[MappingFileError, Seq[ValidatedStatusMapping[A]]] =
      if (errors.nonEmpty) Left(MappingValidationError(MappingType.Status, values, errors))
      else Right(values)
  }

  private object ValidationResults {
    def empty[A]: ValidationResults[A] = ValidationResults[A]()
  }
}
