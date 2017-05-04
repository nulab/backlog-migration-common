package com.nulabinc.backlog.migration.service

import javax.inject.{Inject, Named}

import com.nulabinc.backlog.migration.convert.Convert
import com.nulabinc.backlog.migration.convert.writes.VersionWrites
import com.nulabinc.backlog.migration.domain.BacklogVersion
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.backlog4j.BacklogClient
import com.nulabinc.backlog4j.api.option.{AddVersionParams, UpdateVersionParams}

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class VersionServiceImpl @Inject()(implicit val versionWrites: VersionWrites, @Named("projectKey") projectKey: String, backlog: BacklogClient)
    extends VersionService
    with Logging {

  override def allVersions(): Seq[BacklogVersion] =
    backlog.getVersions(projectKey).asScala.map(Convert.toBacklog(_))

  override def add(backlogVersion: BacklogVersion): Option[BacklogVersion] = {
    val params = new AddVersionParams(projectKey, backlogVersion.name)
    params.description(backlogVersion.description)
    for { startDate <- backlogVersion.optStartDate } yield {
      params.startDate(startDate)
    }
    for { releaseDueDate <- backlogVersion.optReleaseDueDate } yield {
      params.releaseDueDate(releaseDueDate)
    }
    try {
      Some(Convert.toBacklog(backlog.addVersion(params)))
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        None
    }
  }

  override def update(versionId: Long, name: String): Option[BacklogVersion] = {
    val params = new UpdateVersionParams(projectKey, versionId, name)
    try {
      Some(Convert.toBacklog(backlog.updateVersion(params)))
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        None
    }
  }

  override def remove(versionId: Long) = {
    backlog.removeVersion(projectKey, versionId)
  }

}
