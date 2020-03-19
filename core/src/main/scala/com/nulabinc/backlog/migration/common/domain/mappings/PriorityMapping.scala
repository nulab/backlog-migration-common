package com.nulabinc.backlog.migration.common.domain.mappings

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

case class BacklogPriorityMappingItem(private val str: String) {
  val value: String = str.trim
}
