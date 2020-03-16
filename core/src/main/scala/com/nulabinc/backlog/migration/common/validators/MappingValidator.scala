package com.nulabinc.backlog.migration.common.validators

import cats.data.ValidatedNec
import cats.implicits._
import com.nulabinc.backlog.migration.common.domain.mappings.{BacklogStatusMappingItem, StatusMapping, ValidatedStatusMapping}
import com.nulabinc.backlog.migration.common.errors.{DestinationItemNotFound, MappingFileError, MappingValueIsEmpty, MappingValueIsNotSpecified}
import MappingValidatorNec.ValidationResult
import com.nulabinc.backlog.migration.common.domain.{BacklogStatusName, BacklogStatuses}

trait MappingValidator[A] {
  def validateSource(src: A): ValidationResult[A]
}

sealed trait MappingValidatorNec {
  type ValidationResult[A] = ValidatedNec[MappingFileError, A]

  def validateStatusMapping[A](unvalidated: StatusMapping[A], dstItems: BacklogStatuses)
                              (implicit validator: MappingValidator[A]): ValidationResult[ValidatedStatusMapping[A]] =
    (
      validator.validateSource(unvalidated.src),
      validateBacklogStatus(unvalidated.optDst, dstItems)
    ).mapN { (s, d) =>
      new ValidatedStatusMapping[A] {
        override val src: A = s
        override val dst: BacklogStatusMappingItem = d
      }
    }

  private def validateNonEmptyString(value: String): ValidationResult[String] =
    if (value.nonEmpty) value.validNec else MappingValueIsEmpty.invalidNec

  private def validateBacklogStatus(optDst: Option[BacklogStatusMappingItem], dstItems: BacklogStatuses): ValidationResult[BacklogStatusMappingItem] =
    optDst match {
      case Some(dst) =>
        (
          validateNonEmptyString(dst.value),
          if (dstItems.existsByName(BacklogStatusName(dst.value))) BacklogStatusMappingItem(dst.value).validNec
          else DestinationItemNotFound(dst.value).invalidNec
        ).mapN((_, value) => value)
      case None =>
        MappingValueIsNotSpecified.invalidNec
    }
}

object MappingValidatorNec extends MappingValidatorNec
