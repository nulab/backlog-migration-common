package com.nulabinc.backlog.migration.common.errors

import java.nio.file.Path

sealed trait MappingFileError

case class MappingFileNotFound(name: String, path: Path) extends MappingFileError