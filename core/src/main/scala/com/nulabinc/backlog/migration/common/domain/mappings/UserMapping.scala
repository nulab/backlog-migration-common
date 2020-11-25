package com.nulabinc.backlog.migration.common.domain.mappings

trait UserMapping[A] extends Mapping[A] {
  val src: A
  val srcDisplayValue: String
  val optDst: Option[BacklogUserMappingItem]
}

object UserMapping {
  def create[A](srcItem: A): UserMapping[A] =
    new UserMapping[A] {
      override val src: A =
        srcItem
      override val srcDisplayValue: String =
        ""
      override val optDst: Option[BacklogUserMappingItem] =
        None
    }
}

case class BacklogUserMappingItem(private val str: String) {
  val value: String = str.trim
}

trait ValidatedUserMapping[A] extends Mapping[A] {
  val src: A
  val dst: BacklogUserMappingItem
}
