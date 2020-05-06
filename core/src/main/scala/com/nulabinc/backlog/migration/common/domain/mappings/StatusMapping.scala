package com.nulabinc.backlog.migration.common.domain.mappings

trait StatusMapping[A] extends Mapping[A] {
  val src: A
  val srcDisplayValue: String
  val optDst: Option[BacklogStatusMappingItem]
}

object StatusMapping {
  def create[A](srcItem: A): StatusMapping[A] =
    new StatusMapping[A] {
      override val src: A = srcItem
      override val srcDisplayValue: String = ""
      override val optDst: Option[BacklogStatusMappingItem] = None
    }
}

case class BacklogStatusMappingItem(value: String)

trait ValidatedStatusMapping[A] extends Mapping[A] {
  val src: A
  val dst: BacklogStatusMappingItem
}
