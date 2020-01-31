package com.nulabinc.backlog.migration.common.domain.mappings

sealed trait Mapping

case class StatusMapping[A](optSrc: Option[A], optDst: Option[BacklogStatusMappingItem]) extends Mapping

case class BacklogStatusMappingItem(value: MappingValue)