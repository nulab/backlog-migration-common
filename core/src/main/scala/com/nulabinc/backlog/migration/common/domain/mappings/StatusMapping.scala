package com.nulabinc.backlog.migration.common.domain.mappings

trait StatusMapping[A] extends Mapping[A] {
  val src: A
  val optDst: Option[BacklogStatusMappingItem]
}

object StatusMapping {
  def create[A](srcItem: A): StatusMapping[A] =
    new StatusMapping[A] {
      override val src: A = srcItem
      override val optDst: Option[BacklogStatusMappingItem] = None
    }
}

case class BacklogStatusMappingItem(value: String) extends AnyVal

trait ValidatedStatusMapping[A] {
  val src: A
  val dst: BacklogStatusMappingItem
}