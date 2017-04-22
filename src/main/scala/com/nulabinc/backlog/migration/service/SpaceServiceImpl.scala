package com.nulabinc.backlog.migration.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.converter.Backlog4jConverters
import com.nulabinc.backlog.migration.domain.{BacklogEnvironment, BacklogSpace}
import com.nulabinc.backlog4j.BacklogClient

/**
  * @author uchida
  */
class SpaceServiceImpl @Inject()(backlog: BacklogClient) extends SpaceService {

  override def space(): BacklogSpace =
    Backlog4jConverters.Space(backlog.getSpace)

  override def hasAdmin(): Boolean =
    try {
      backlog.getSpaceDiskUsage
      true
    } catch {
      case _: Throwable => false
    }

  override def environment(): BacklogEnvironment =
    Backlog4jConverters.Environment(backlog.getEnvironment)

}
