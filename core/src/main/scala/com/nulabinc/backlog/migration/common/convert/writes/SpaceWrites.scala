package com.nulabinc.backlog.migration.common.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.Writes
import com.nulabinc.backlog.migration.common.domain.BacklogSpace
import com.nulabinc.backlog.migration.common.utils.{DateUtil, Logging}
import com.nulabinc.backlog4j.Space

/**
 * @author uchida
 */
private[common] class SpaceWrites @Inject() () extends Writes[Space, BacklogSpace] with Logging {

  override def writes(space: Space): BacklogSpace = {
    BacklogSpace(
      spaceKey = space.getSpaceKey,
      name = space.getName,
      created = DateUtil.isoFormat(space.getCreated)
    )
  }

}
