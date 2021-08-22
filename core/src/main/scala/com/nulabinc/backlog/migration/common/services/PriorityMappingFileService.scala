package com.nulabinc.backlog.migration.common.services

import java.nio.file.Path

import cats.Foldable.ops._
import cats.Monad
import cats.Monad.ops._
import cats.data.Validated.{Invalid, Valid}
import com.nulabinc.backlog.migration.common.codec.{PriorityMappingDecoder, PriorityMappingEncoder}
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
import com.nulabinc.backlog4j.Priority

object PriorityMappingFileService {
  import com.nulabinc.backlog.migration.common.messages.ConsoleMessages.{
    Mappings => MappingMessages
  }
  import com.nulabinc.backlog.migration.common.shared.syntax._

  def init[A, F[_]: Monad: StorageDSL: ConsoleDSL](
      mappingFilePath: Path,
      mappingListPath: Path,
      srcItems: Seq[A],
      dstItems: Seq[Priority]
  )(implicit
      formatter: Formatter[PriorityMapping[A]],
      encoder: PriorityMappingEncoder[A],
      decoder: PriorityMappingDecoder[A],
      header: MappingHeader[PriorityMapping[_]]
  ): F[Unit] =
    for {
      exists <- StorageDSL[F].exists(mappingFilePath)
      _ <-
        if (exists) {
          for {
            records <- StorageDSL[F].read(mappingFilePath, MappingFileService.readLine)
            mappings = MappingDecoder.priority(records)
            result   = merge(mappings, srcItems)
            _ <-
              if (result.addedList.nonEmpty)
                for {
                  _ <- StorageDSL[F].writeNewFile(
                    mappingFilePath,
                    MappingEncoder.priority(result.mergeList)
                  )
                  _ <- ConsoleDSL[F].println(
                    MappingMessages.priorityMappingMerged(mappingFilePath, result.addedList)
                  )
                } yield ()
              else
                ConsoleDSL[F].println(MappingMessages.priorityMappingNoChanges)
          } yield ()
        } else {
          val result = merge(Seq(), srcItems)
          for {
            _ <- StorageDSL[F].writeNewFile(
              mappingFilePath,
              MappingEncoder.priority(result.mergeList)
            )
            _ <- ConsoleDSL[F].println(
              MappingMessages.priorityMappingCreated(mappingFilePath)
            )
          } yield ()
        }
      _ <- StorageDSL[F].writeNewFile(
        mappingListPath,
        MappingEncoder.priorityList(dstItems)
      )
    } yield ()

  /**
   * List of items that can be specified in Priorityes.csv
   *
   * @param path
   * @param dstItems
   * @param decoder
   * @tparam A
   * @tparam F
   * @return
   */
  def execute[A, F[_]: Monad: StorageDSL: ConsoleDSL](
      path: Path,
      dstItems: Seq[Priority]
  )(implicit
      decoder: PriorityMappingDecoder[A]
  ): F[Either[MappingFileError, Seq[ValidatedPriorityMapping[A]]]] = {
    val result = for {
      _ <- StorageDSL[F].exists(path).orError(MappingFileNotFound("priority", path)).handleError
      unvalidated <- getMappings(path).handleError
      validated   <- validateMappings(unvalidated, dstItems).lift.handleError
    } yield validated

    result.value
  }

  /**
   * Deserialize a mapping file.
   *
   * @param path
   * @param decoder
   * @tparam A
   * @tparam F
   * @return
   */
  def getMappings[A, F[_]: Monad: ConsoleDSL: StorageDSL](path: Path)(implicit
      decoder: PriorityMappingDecoder[A]
  ): F[Either[MappingFileError, Seq[PriorityMapping[A]]]] =
    for {
      records <- StorageDSL[F].read(path, MappingFileService.readLine)
      mappings = MappingDecoder.priority(records)
    } yield Right(mappings)

  /**
   * Validate mappings
   * @param mappings
   * @param dstItems
   * @tparam A
   * @return
   */
  def validateMappings[A](
      mappings: Seq[PriorityMapping[A]],
      dstItems: Seq[Priority]
  ): Either[MappingFileError, Seq[ValidatedPriorityMapping[A]]] = {
    val results = mappings
      .map(MappingValidatorNec.validatePriorityMapping(_, dstItems))
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
   * @param mappings
   * @param srcItems
   * @tparam A
   * @return
   */
  private def merge[A](
      mappings: Seq[PriorityMapping[A]],
      srcItems: Seq[A]
  ): MergedPriorityMapping[A] =
    srcItems.foldLeft(MergedPriorityMapping.empty[A]) { (acc, item) =>
      mappings.find(_.src == item) match {
        case Some(value) =>
          acc.copy(mergeList = acc.mergeList :+ value)
        case None =>
          val mapping = PriorityMapping.create(item)
          acc.copy(
            mergeList = acc.mergeList :+ mapping,
            addedList = acc.addedList :+ mapping
          )
      }
    }

  private case class ValidationResults[A](
      values: Seq[ValidatedPriorityMapping[A]] = Seq(),
      errors: List[ValidationError] = List()
  ) {
    def toResult: Either[MappingFileError, Seq[ValidatedPriorityMapping[A]]] =
      if (errors.nonEmpty) Left(MappingValidationError(MappingType.Priority, values, errors))
      else Right(values)
  }

  private object ValidationResults {
    def empty[A]: ValidationResults[A] = ValidationResults[A]()
  }
}

private case class MergedPriorityMapping[A](
    mergeList: Seq[PriorityMapping[A]],
    addedList: Seq[PriorityMapping[A]]
)

private object MergedPriorityMapping {
  def empty[A]: MergedPriorityMapping[A] =
    MergedPriorityMapping[A](mergeList = Seq(), addedList = Seq())
}
