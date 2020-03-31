package com.nulabinc.backlog.migration.common.domain.mappings

trait UserMapping[A] extends Mapping[A] {
  val src: A
  val optDst: Option[BacklogUserMappingItem]
  val mappingType: String
}

object UserMapping {
  def create[A](srcItem: A, isNAISpace: Boolean): UserMapping[A] =
    new UserMapping[A] {
      override val src: A =
        srcItem
      override val optDst: Option[BacklogUserMappingItem] =
        None
      override val mappingType: String =
        if (isNAISpace) MailUserMappingType.value
        else IdUserMappingType.value
    }
}

sealed abstract class UserMappingType(val value: String)
case object IdUserMappingType extends UserMappingType("id")
case object MailUserMappingType extends UserMappingType("mail")

object UserMappingType {
  def from(str: String): UserMappingType =
    str match {
      case "id" => IdUserMappingType
      case "mail" => MailUserMappingType
      case others => throw new RuntimeException(s"Invalid user mapping type. Input: $others")
    }
}

case class BacklogUserMappingItem(private val str: String) {
  val value: String = str.trim
}

trait ValidatedUserMapping[A] extends Mapping[A] {
  val src: A
  val dst: BacklogUserMappingItem
  val mappingType: UserMappingType
}