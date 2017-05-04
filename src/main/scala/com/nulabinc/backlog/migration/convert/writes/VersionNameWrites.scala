package com.nulabinc.backlog.migration.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.convert.Writes
import com.nulabinc.backlog.migration.domain.BacklogVersion
import com.nulabinc.backlog.migration.utils.Logging

/**
  * @author uchida
  */
class VersionNameWrites @Inject()() extends Writes[String, BacklogVersion] with Logging {

  override def writes(name: String): BacklogVersion = {
    BacklogVersion(
      optId = None,
      name = name,
      description = "",
      optStartDate = None,
      optReleaseDueDate = None,
      delete = true
    )
  }

}
