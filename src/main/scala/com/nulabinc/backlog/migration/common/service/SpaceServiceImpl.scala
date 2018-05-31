package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog.migration.common.client.BacklogAPIClient
import javax.inject.Inject
import com.nulabinc.backlog.migration.common.convert.Convert
import com.nulabinc.backlog.migration.common.convert.writes.SpaceWrites
import com.nulabinc.backlog.migration.common.domain.BacklogSpace

/**
  * @author uchida
  */
class SpaceServiceImpl @Inject()(implicit val spaceWrites: SpaceWrites, implicit val backlog: BacklogAPIClient)
    extends SpaceService {

  override def space(): BacklogSpace =
    Convert.toBacklog(backlog.getSpace)

  override def hasAdmin(): Boolean =
    try {
      backlog.getSpaceDiskUsage
      true
    } catch {
      case _: Throwable => false
    }

}
