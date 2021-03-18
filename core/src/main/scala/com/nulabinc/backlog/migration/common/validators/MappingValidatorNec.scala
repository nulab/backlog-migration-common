package com.nulabinc.backlog.migration.common.validators

import cats.data.ValidatedNec
import cats.implicits._
import com.nulabinc.backlog.migration.common.domain.mappings._
import com.nulabinc.backlog.migration.common.domain.{
  BacklogStatusName,
  BacklogStatuses,
  BacklogUser
}
import com.nulabinc.backlog.migration.common.errors.{DestinationItemNotFound, MappingValueIsEmpty, MappingValueIsNotSpecified, ValidationError}
import com.nulabinc.backlog4j.Priority

sealed trait MappingValidatorNec {
  type ValidationResult[A] = ValidatedNec[ValidationError, A]

  def validatePriorityMapping[A](
      unvalidated: PriorityMapping[A],
      dstItems: Seq[Priority]
  ): ValidationResult[ValidatedPriorityMapping[A]] =
    validateBacklogPriority(unvalidated, dstItems).map { d =>
      new ValidatedPriorityMapping[A] {
        override val src: A                          = unvalidated.src
        override val srcDisplayValue: String         = unvalidated.srcDisplayValue
        override val dst: BacklogPriorityMappingItem = d
      }
    }

  def validateStatusMapping[A](
      unvalidated: StatusMapping[A],
      dstItems: BacklogStatuses
  ): ValidationResult[ValidatedStatusMapping[A]] =
    validateBacklogStatus(unvalidated, dstItems).map { d =>
      new ValidatedStatusMapping[A] {
        override val src: A                        = unvalidated.src
        override val srcDisplayValue: String       = unvalidated.srcDisplayValue
        override val dst: BacklogStatusMappingItem = d
      }
    }

  def validateUserMapping[A](
      unvalidated: UserMapping[A],
      dstItems: Seq[BacklogUser]
  ): ValidationResult[ValidatedUserMapping[A]] =
    validateBacklogUser(unvalidated, dstItems).map { result =>
      new ValidatedUserMapping[A] {
        override val src: A                      = unvalidated.src
        override val srcDisplayValue: String     = unvalidated.srcDisplayValue
        override val dst: BacklogUserMappingItem = result
      }
    }

  private def validateNonEmptyString[A](
      mapping: Mapping[A],
      value: String
  ): ValidationResult[String] =
    if (value.nonEmpty) value.validNec
    else MappingValueIsEmpty(mapping).invalidNec

  private def validateBacklogPriority[A](
      mapping: PriorityMapping[A],
      dstItems: Seq[Priority]
  ): ValidationResult[BacklogPriorityMappingItem] =
    mapping.optDst match {
      case Some(dst) =>
        validateNonEmptyString(mapping, dst.value).andThen { value =>
          if (dstItems.exists(_.getName == value))
            BacklogPriorityMappingItem(dst.value).validNec
          else DestinationItemNotFound(dst.value).invalidNec
        }
      case None =>
        MappingValueIsNotSpecified(mapping).invalidNec
    }

  private def validateBacklogStatus[A](
      mapping: StatusMapping[A],
      dstItems: BacklogStatuses
  ): ValidationResult[BacklogStatusMappingItem] =
    mapping.optDst match {
      case Some(dst) =>
        validateNonEmptyString(mapping, dst.value).andThen { value =>
          if (dstItems.existsByName(BacklogStatusName(value)))
            BacklogStatusMappingItem(dst.value).validNec
          else DestinationItemNotFound(dst.value).invalidNec
        }
      case None =>
        MappingValueIsNotSpecified(mapping).invalidNec
    }

  private def validateBacklogUser[A](
      mapping: UserMapping[A],
      dstItems: Seq[BacklogUser]
  ): ValidationResult[BacklogUserMappingItem] =
    mapping.optDst
      .map(_.validNec)
      .getOrElse(MappingValueIsNotSpecified(mapping).invalidNec)
      .andThen { dst =>
        validateNonEmptyString(mapping, dst.value).andThen { value =>
          BacklogUserMappingItem(value).validNec
        }
      }

}

object MappingValidatorNec extends MappingValidatorNec
