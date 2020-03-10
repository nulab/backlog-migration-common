package com.nulabinc.backlog.migration.common.domain.mappings

trait UserMapping[A] extends Mapping[A] {
  val src: A
  val optDst: Option[BacklogUserMappingItem]
}

object UserMapping {
  def create[A](srcItem: A): UserMapping[A] =
    new UserMapping[A] {
      override val src: A = srcItem
      override val optDst: Option[BacklogUserMappingItem] = None
    }
}

sealed trait BacklogUserMappingItem{
  val value: String
  val mappingType: String
}

object BacklogUserMappingItem {
  def from(value: String, mappingType: String): BacklogUserMappingItem =
    mappingType match {
      case "id" => BacklogUserIdMappingItem(value)
      case "mail" => BacklogUserMailMappingItem(value)
      case others => throw new RuntimeException(s"Invalid user mapping item. Type: $others") // TODO
    }
}

case class BacklogUserIdMappingItem(value: String) extends BacklogUserMappingItem {
  override val mappingType: String = "id"
}
case class BacklogUserMailMappingItem(value: String) extends BacklogUserMappingItem {
  override val mappingType: String = "mail"
}
