package com.nulabinc.backlog.migration.common.services

import java.nio.file.Path

import cats.Foldable.ops._
import cats.Monad
import cats.Monad.ops._
import cats.data.Validated.{Invalid, Valid}
import com.nulabinc.backlog.migration.common.conf.BacklogApiConfiguration
import com.nulabinc.backlog.migration.common.deserializers.Deserializer
import com.nulabinc.backlog.migration.common.domain.BacklogUser
import com.nulabinc.backlog.migration.common.domain.mappings._
import com.nulabinc.backlog.migration.common.dsl.{ConsoleDSL, StorageDSL}
import com.nulabinc.backlog.migration.common.errors.{
  MappingFileError,
  MappingFileNotFound,
  MappingValidationError,
  ValidationError
}
import com.nulabinc.backlog.migration.common.formatters.Formatter
import com.nulabinc.backlog.migration.common.serializers.Serializer
import com.nulabinc.backlog.migration.common.validators.MappingValidatorNec
import org.apache.commons.csv.CSVRecord

private case class MergedUserMapping[A](
    mergeList: Seq[UserMapping[A]],
    addedList: Seq[UserMapping[A]]
)

private object MergedUserMapping {
  def empty[A]: MergedUserMapping[A] =
    MergedUserMapping[A](mergeList = Seq(), addedList = Seq())
}

object UserMappingFileService {
  import com.nulabinc.backlog.migration.common.messages.ConsoleMessages.{
    Mappings => MappingMessages
  }
  import com.nulabinc.backlog.migration.common.shared.syntax._

  def init[A, F[_]: Monad: StorageDSL: ConsoleDSL](
      mappingFilePath: Path,
      mappingListPath: Path,
      srcItems: Seq[A],
      dstItems: Seq[BacklogUser],
      dstApiConfiguration: BacklogApiConfiguration
  )(implicit
      formatter: Formatter[UserMapping[A]],
      serializer: Serializer[UserMapping[A], Seq[String]],
      deserializer: Deserializer[CSVRecord, UserMapping[A]],
      header: MappingHeader[UserMapping[_]]
  ): F[Unit] =
    for {
      mappingFileExists <- StorageDSL[F].exists(mappingFilePath)
      _ <-
        if (mappingFileExists) {
          for {
            records <-
              StorageDSL[F].read(mappingFilePath, MappingFileService.readLine)
            mappings = MappingDeserializer.user(records)
            result = merge(mappings, srcItems, dstApiConfiguration.isNAISpace)
            _ <-
              if (result.addedList.nonEmpty)
                for {
                  _ <- StorageDSL[F].writeNewFile(
                    mappingFilePath,
                    MappingSerializer.user(result.mergeList)
                  )
                  _ <- ConsoleDSL[F].println(
                    MappingMessages
                      .userMappingMerged(mappingFilePath, result.addedList)
                  )
                } yield ()
              else
                ConsoleDSL[F].println(MappingMessages.userMappingNoChanges)
          } yield ()
        } else {
          val result = merge(Seq(), srcItems, dstApiConfiguration.isNAISpace)
          for {
            _ <- StorageDSL[F].writeNewFile(
              mappingFilePath,
              MappingSerializer.user(result.mergeList)
            )
            _ <- ConsoleDSL[F].println(
              MappingMessages.userMappingCreated(mappingFilePath)
            )
          } yield ()
        }
      _ <- StorageDSL[F].writeNewFile(
        mappingListPath,
        MappingSerializer.userList(dstItems)
      )
    } yield ()

  /**
    * List of items that can be specified in users.csv
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
      dstItems: Seq[BacklogUser]
  )(implicit
      deserializer: Deserializer[CSVRecord, UserMapping[A]]
  ): F[Either[MappingFileError, Seq[ValidatedUserMapping[A]]]] = {
    val result = for {
      _ <-
        StorageDSL[F]
          .exists(path)
          .orError(MappingFileNotFound("users", path))
          .handleError
      unvalidated <- getMappings(path).handleError
      validated <- validateMappings(unvalidated, dstItems).lift.handleError
    } yield validated

    result.value
  }

  /**
    * Deserialize user mappings from a mapping file.
    *
   * @param path
    * @param deserializer
    * @tparam A
    * @tparam F
    * @return
    */
  def getMappings[A, F[_]: Monad: StorageDSL](path: Path)(implicit
      deserializer: Deserializer[CSVRecord, UserMapping[A]]
  ): F[Either[MappingFileError, Seq[UserMapping[A]]]] =
    for {
      records <- StorageDSL[F].read(path, MappingFileService.readLine)
      mappings = MappingDeserializer.user(records)
    } yield Right(mappings)

  /**
    * Validate mappings
    * @param mappings
    * @param dstItems
    * @tparam A
    * @return
    */
  def validateMappings[A](
      mappings: Seq[UserMapping[A]],
      dstItems: Seq[BacklogUser]
  ): Either[MappingFileError, Seq[ValidatedUserMapping[A]]] = {
    val results = mappings
      .map(MappingValidatorNec.validateUserMapping(_, dstItems))
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
      mappings: Seq[UserMapping[A]],
      srcItems: Seq[A],
      isNAISpace: Boolean
  ): MergedUserMapping[A] =
    srcItems.foldLeft(MergedUserMapping.empty[A]) { (acc, item) =>
      mappings.find(_.src == item) match {
        case Some(value) =>
          acc.copy(mergeList = acc.mergeList :+ value)
        case None =>
          val mapping = UserMapping.create(item, isNAISpace)
          acc.copy(
            mergeList = acc.mergeList :+ mapping,
            addedList = acc.addedList :+ mapping
          )
      }
    }

  private case class ValidationResults[A](
      values: Seq[ValidatedUserMapping[A]] = Seq(),
      errors: List[ValidationError] = List()
  ) {
    def toResult: Either[MappingFileError, Seq[ValidatedUserMapping[A]]] =
      if (errors.nonEmpty) Left(MappingValidationError(values, errors))
      else Right(values)
  }

  private object ValidationResults {
    def empty[A]: ValidationResults[A] = ValidationResults[A]()
  }

}
