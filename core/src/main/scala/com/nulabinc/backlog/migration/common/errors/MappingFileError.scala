package com.nulabinc.backlog.migration.common.errors

import java.nio.file.Path

import com.nulabinc.backlog.migration.common.domain.mappings.Mapping

sealed trait MappingFileError

case class MappingFileNotFound(name: String, path: Path) extends MappingFileError
case class MappingValidationError[A](mappings: Seq[Mapping[A]], errors: List[ValidationError]) extends MappingFileError


sealed trait ValidationError

case class MappingValueIsEmpty[A](mapping: Mapping[A]) extends ValidationError
case object MappingValueIsNotSpecified extends ValidationError
case class DestinationItemNotFound(value: String) extends ValidationError
case class InvalidItemValue(required: String, input: String) extends ValidationError