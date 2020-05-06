package com.nulabinc.backlog.migration.common.domain.exports

import com.nulabinc.backlog.migration.common.domain.{
  BacklogStatus,
  BacklogStatusName
}

sealed trait ExportedBacklogStatus {
  val name: BacklogStatusName
}

case class ExistingExportedBacklogStatus(status: BacklogStatus)
    extends ExportedBacklogStatus {
  override val name: BacklogStatusName = status.name
}

case class DeletedExportedBacklogStatus(name: BacklogStatusName)
    extends ExportedBacklogStatus
