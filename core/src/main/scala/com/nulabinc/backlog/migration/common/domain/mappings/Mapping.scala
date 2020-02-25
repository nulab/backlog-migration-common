package com.nulabinc.backlog.migration.common.domain.mappings

trait Mapping[A]

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

trait PriorityMapping[A] extends Mapping[A] {
  val src: A
  val optDst: Option[BacklogPriorityMappingItem]
}

object PriorityMapping {
  def create[A](srcItem: A): PriorityMapping[A] =
    new PriorityMapping[A] {
      override val src: A = srcItem
      override val optDst: Option[BacklogPriorityMappingItem] = None
    }
}

case class BacklogStatusMappingItem(value: String) extends AnyVal
case class BacklogPriorityMappingItem(value: String) extends AnyVal