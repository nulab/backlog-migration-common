package com.nulabinc.backlog.migration.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.convert.Writes
import com.nulabinc.backlog.migration.domain.BacklogSpace
import com.nulabinc.backlog.migration.utils.{DateUtil, Logging}
import com.nulabinc.backlog4j.Space

/**
  * @author uchida
  */
class SpaceWrites @Inject()() extends Writes[Space, BacklogSpace] with Logging {

  override def writes(space: Space): BacklogSpace = {
    BacklogSpace(
      spaceKey = space.getSpaceKey,
      name = space.getName,
      created = DateUtil.isoFormat(space.getCreated)
    )
  }

}
