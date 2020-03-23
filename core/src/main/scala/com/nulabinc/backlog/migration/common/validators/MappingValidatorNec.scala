package com.nulabinc.backlog.migration.common.validators

import cats.data.ValidatedNec
import cats.implicits._
import com.nulabinc.backlog.migration.common.domain.mappings.{BacklogStatusMappingItem, StatusMapping, ValidatedStatusMapping}
import com.nulabinc.backlog.migration.common.domain.{BacklogStatusName, BacklogStatuses}
import com.nulabinc.backlog.migration.common.errors.{DestinationItemNotFound, MappingValueIsEmpty, MappingValueIsNotSpecified, ValidationError}

sealed trait MappingValidatorNec {
  type ValidationResult[A] = ValidatedNec[ValidationError, A]

  def validateStatusMapping[A](unvalidated: StatusMapping[A], dstItems: BacklogStatuses): ValidationResult[ValidatedStatusMapping[A]] =
    validateBacklogStatus(unvalidated.optDst, dstItems).map { d =>
      new ValidatedStatusMapping[A] {
        override val src: A = unvalidated.src
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
