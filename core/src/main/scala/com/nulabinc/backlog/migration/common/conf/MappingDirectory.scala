package com.nulabinc.backlog.migration.common.conf

import java.io.File
import java.nio.file.Path

case class MappingDirectory(
    userMappingFilePath: Path,
    userMappingListFilePath: Path,
    statusMappingFilePath: Path,
    statusMappingListFilePath: Path,
    priorityMappingFilePath: Path,
    priorityMappingListFilePath: Path
)

object MappingDirectory {
  private val WORKING_DIRECTORY = new File(".").getAbsoluteFile.getParent
  private val ROOT              = WORKING_DIRECTORY + "/mapping"

  private final val USER_MAPPING_FILE          = ROOT + "/users.json"
  private final val STATUS_MAPPING_FILE        = ROOT + "/statuses.csv"
  private final val PRIORITY_MAPPING_FILE      = ROOT + "/priorities.json"
  private final val USER_MAPPING_LIST_FILE     = ROOT + "/users_list.csv"
  private final val STATUS_MAPPING_LIST_FILE   = ROOT + "/statuses_list.csv"
  private final val PRIORITY_MAPPING_LIST_FILE = ROOT + "/priorities_list.csv"

  val default: MappingDirectory = MappingDirectory(
    userMappingFilePath = toAbsolutePath(USER_MAPPING_FILE),
    userMappingListFilePath = toAbsolutePath(USER_MAPPING_LIST_FILE),
    statusMappingFilePath = toAbsolutePath(STATUS_MAPPING_FILE),
    statusMappingListFilePath = toAbsolutePath(STATUS_MAPPING_LIST_FILE),
    priorityMappingFilePath = toAbsolutePath(PRIORITY_MAPPING_FILE),
    priorityMappingListFilePath = toAbsolutePath(PRIORITY_MAPPING_LIST_FILE)
  )

  private def toAbsolutePath(str: String): Path =
    new File(str).getAbsoluteFile.toPath
}
