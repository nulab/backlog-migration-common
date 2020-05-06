package com.nulabinc.backlog.migration.common.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.Writes
import com.nulabinc.backlog.migration.common.domain.BacklogVersion
import com.nulabinc.backlog.migration.common.utils.Logging

/**
  * @author uchida
  */
class VersionNameWrites @Inject() ()
    extends Writes[String, BacklogVersion]
    with Logging {

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
