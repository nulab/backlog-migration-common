package com.nulabinc.backlog.migration.common.errors

import java.nio.file.Path

import com.nulabinc.backlog.migration.common.domain.mappings.{Mapping, MappingType}

sealed trait MappingFileError

case class MappingFileNotFound(name: String, path: Path) extends MappingFileError
case class MappingValidationError[A](
    mappingType: MappingType,
    mappings: Seq[Mapping[A]],
    errors: List[ValidationError]
) extends MappingFileError

sealed trait ValidationError

case class MappingValueIsEmpty[A](mapping: Mapping[A])        extends ValidationError
case class MappingValueIsNotSpecified[A](mapping: Mapping[A]) extends ValidationError
case class DestinationItemNotFound(value: String)             extends ValidationError
case class InvalidItemValue(required: String, input: String)  extends ValidationError
