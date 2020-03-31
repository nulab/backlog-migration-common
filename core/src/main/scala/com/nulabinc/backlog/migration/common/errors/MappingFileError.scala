package com.nulabinc.backlog.migration.common.errors

import java.nio.file.Path

sealed trait MappingFileError

case class MappingFileNotFound(name: String, path: Path) extends MappingFileError
case class MappingValidationError(errors: List[ValidationError]) extends MappingFileError


sealed trait ValidationError

case object MappingValueIsEmpty extends ValidationError
case object MappingValueIsNotSpecified extends ValidationError
case class DestinationItemNotFound(value: String) extends ValidationError
case class InvalidItemValue(required: String, input: String) extends ValidationError