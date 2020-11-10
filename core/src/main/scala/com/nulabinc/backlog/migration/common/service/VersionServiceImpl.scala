package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog.migration.common.client.BacklogAPIClient
import javax.inject.Inject
import com.nulabinc.backlog.migration.common.convert.Convert
import com.nulabinc.backlog.migration.common.convert.writes.VersionWrites
import com.nulabinc.backlog.migration.common.domain.{BacklogProjectKey, BacklogVersion}
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog4j.api.option.{AddVersionParams, UpdateVersionParams}

import scala.jdk.CollectionConverters._

/**
 * @author uchida
 */
class VersionServiceImpl @Inject() (implicit
    val versionWrites: VersionWrites,
    projectKey: BacklogProjectKey,
    backlog: BacklogAPIClient
) extends VersionService
    with Logging {

  override def allVersions(): Seq[BacklogVersion] =
    backlog.getVersions(projectKey.value).asScala.toSeq.map(Convert.toBacklog(_))

  override def add(backlogVersion: BacklogVersion): Option[BacklogVersion] = {
    val params = new AddVersionParams(projectKey.value, backlogVersion.name)
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
    val params = new UpdateVersionParams(projectKey.value, versionId, name)
    try {
      Some(Convert.toBacklog(backlog.updateVersion(params)))
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        None
    }
  }

  override def remove(versionId: Long) = {
    backlog.removeVersion(projectKey.value, versionId)
  }

}
