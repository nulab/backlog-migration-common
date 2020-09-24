package com.nulabinc.backlog.migration.common.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.Writes
import com.nulabinc.backlog.migration.common.domain.BacklogVersion
import com.nulabinc.backlog.migration.common.utils.{DateUtil, Logging}
import com.nulabinc.backlog4j.Version

/**
  * @author uchida
  */
class VersionWrites @Inject() () extends Writes[Version, BacklogVersion] with Logging {

  override def writes(version: Version): BacklogVersion = {
    BacklogVersion(
      optId = Some(version.getId),
      name = version.getName,
      description = Option(version.getDescription).getOrElse(""),
      optStartDate = Option(version.getStartDate).map(DateUtil.dateFormat),
      optReleaseDueDate = Option(version.getReleaseDueDate).map(DateUtil.dateFormat),
      delete = false
    )
  }

}
