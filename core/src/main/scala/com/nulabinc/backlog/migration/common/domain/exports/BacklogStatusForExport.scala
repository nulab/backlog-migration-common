package com.nulabinc.backlog.migration.common.domain.exports

import com.nulabinc.backlog.migration.common.domain.{BacklogStatus, BacklogStatusName}

sealed trait BacklogStatusForExport{
  val name: BacklogStatusName
}

case class ExistingBacklogStatus(status: BacklogStatus) extends BacklogStatusForExport {
  override val name: BacklogStatusName = status.name
}

case class DeletedBacklogStatus(name: BacklogStatusName) extends BacklogStatusForExport

