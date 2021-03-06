package com.nulabinc.backlog.migration.common.domain.mappings

trait PriorityMapping[A] extends Mapping[A] {
  val src: A
  val srcDisplayValue: String
  val optDst: Option[BacklogPriorityMappingItem]
}

object PriorityMapping {
  def create[A](srcItem: A): PriorityMapping[A] =
    new PriorityMapping[A] {
      override val src: A                                     = srcItem
      override val srcDisplayValue: String                    = ""
      override val optDst: Option[BacklogPriorityMappingItem] = None
    }
}

case class BacklogPriorityMappingItem(value: String)

trait ValidatedPriorityMapping[A] extends Mapping[A] {
  val src: A
  val dst: BacklogPriorityMappingItem
}
