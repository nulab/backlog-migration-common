package com.nulabinc.backlog.migration.common.validators

import cats.data.ValidatedNec
import cats.implicits._
import com.nulabinc.backlog.migration.common.domain.mappings._
import com.nulabinc.backlog.migration.common.domain.{BacklogStatusName, BacklogStatuses, BacklogUser}
import com.nulabinc.backlog.migration.common.errors.{DestinationItemNotFound, InvalidItemValue, MappingValueIsEmpty, MappingValueIsNotSpecified, ValidationError}
import com.nulabinc.backlog4j.Priority

sealed trait MappingValidatorNec {
  type ValidationResult[A] = ValidatedNec[ValidationError, A]

  def validatePriorityMapping[A](unvalidated: PriorityMapping[A], dstItems: Seq[Priority]): ValidationResult[ValidatedPriorityMapping[A]] =
    validateBacklogPriority(unvalidated.optDst, dstItems).map { d =>
      new ValidatedPriorityMapping[A] {
        override val src: A = unvalidated.src
        override val dst: BacklogPriorityMappingItem = d
      }
    }

  def validateStatusMapping[A](unvalidated: StatusMapping[A], dstItems: BacklogStatuses): ValidationResult[ValidatedStatusMapping[A]] =
    validateBacklogStatus(unvalidated.optDst, dstItems).map { d =>
      new ValidatedStatusMapping[A] {
        override val src: A = unvalidated.src
        override val dst: BacklogStatusMappingItem = d
      }
    }

  def validateUserMapping[A](unvalidated: UserMapping[A], dstItems: Seq[BacklogUser]): ValidationResult[ValidatedUserMapping[A]] =
    validateBacklogUser(unvalidated.optDst, unvalidated.mappingType, dstItems).map { result =>
      new ValidatedUserMapping[A] {
        override val src: A = unvalidated.src
        override val dst: BacklogUserMappingItem = result._2
        override val mappingType: UserMappingType = result._1
      }
    }

  private def validateNonEmptyString(value: String): ValidationResult[String] =
    if (value.nonEmpty) value.validNec else MappingValueIsEmpty.invalidNec

  private def validateValueIsDefined[A](optValue: Option[A]): ValidationResult[A] =
    optValue.map(_.validNec).getOrElse(MappingValueIsNotSpecified.invalidNec)

  private def validateBacklogPriority(optDst: Option[BacklogPriorityMappingItem], dstItems: Seq[Priority]): ValidationResult[BacklogPriorityMappingItem] =
    optDst match {
      case Some(dst) =>
        (
          validateNonEmptyString(dst.value),
          if (dstItems.exists(_.getName == dst.value)) BacklogPriorityMappingItem(dst.value).validNec
          else DestinationItemNotFound(dst.value).invalidNec
        ).mapN((_, value) => value)
      case None =>
        MappingValueIsNotSpecified.invalidNec
    }

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

  private def validateBacklogUser(optDst: Option[BacklogUserMappingItem], mappingTypeStr: String, dstItems: Seq[BacklogUser]): ValidationResult[(UserMappingType, BacklogUserMappingItem)] =
    validateValueIsDefined(optDst).andThen { dst =>
      validateUserMappingType(mappingTypeStr).andThen { mappingType =>
        (
          validateNonEmptyString(dst.value),
          validateUserMappingType(mappingTypeStr),
          mappingType match {
            case IdUserMappingType =>
              if (dstItems.map(_.optUserId).exists(_.contains(dst.value))) dst.validNec
              else DestinationItemNotFound(dst.value).invalidNec
            case MailUserMappingType =>
              if (dstItems.map(_.optMailAddress).exists(_.contains(dst.value))) dst.validNec
              else DestinationItemNotFound(dst.value).invalidNec
          }
        ).mapN((_, mappingType, dstValue) => (mappingType, dstValue))
      }
    }

  private def validateUserMappingType(str: String): ValidationResult[UserMappingType] =
    str match {
      case "id" => IdUserMappingType.valid
      case "mail" => MailUserMappingType.valid
      case others => InvalidItemValue("id or mail", others).invalidNec
    }
}

object MappingValidatorNec extends MappingValidatorNec
