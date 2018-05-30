package com.nulabinc.backlog.migration.common.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.Convert
import com.nulabinc.backlog.migration.common.convert.writes.SpaceWrites
import com.nulabinc.backlog.migration.common.domain.BacklogSpace
import com.nulabinc.backlog4j.BacklogClient

/**
  * @author uchida
  */
class SpaceServiceImpl @Inject()(implicit val spaceWrites: SpaceWrites, implicit val backlog: BacklogClient)
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
