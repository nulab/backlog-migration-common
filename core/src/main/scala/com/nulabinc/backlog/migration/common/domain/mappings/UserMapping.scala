package com.nulabinc.backlog.migration.common.domain.mappings

trait UserMapping[A] extends Mapping[A] {
  val src: A
  val dst: BacklogUserMappingItem
}

object UserMapping {
  def create[A](srcItem: A, isNAISpace: Boolean): UserMapping[A] =
    new UserMapping[A] {
      override val src: A =
        srcItem
      override val dst: BacklogUserMappingItem =
        if (isNAISpace) BacklogUserMappingItem.from(None, "mail")
        else BacklogUserMappingItem.from(None, "id")
    }
}

sealed trait BacklogUserMappingItem{
  val optValue: Option[String]
  val mappingType: String
}

object BacklogUserMappingItem {
  def from(optValue: Option[String], mappingType: String): BacklogUserMappingItem =
    mappingType match {
      case "id" => BacklogUserIdMappingItem(optValue)
      case "mail" => BacklogUserMailMappingItem(optValue)
      case others => throw new RuntimeException(s"Invalid user mapping item. Type: $others") // TODO
    }
}

case class BacklogUserIdMappingItem(private val optStr: Option[String]) extends BacklogUserMappingItem {
  override val optValue: Option[String] = optStr.map(_.trim)
  override val mappingType: String = "id"
}
case class BacklogUserMailMappingItem(private val optStr: Option[String]) extends BacklogUserMappingItem {
  override val optValue: Option[String] = optStr.map(_.trim)
  override val mappingType: String = "mail"
}
