package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog.migration.common.client.BacklogAPIClient

import javax.inject.Inject
import com.nulabinc.backlog.migration.common.convert.Convert
import com.nulabinc.backlog.migration.common.convert.writes.UserWrites
import com.nulabinc.backlog.migration.common.domain.{BacklogProjectKey, BacklogUser}
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog4j.BacklogAPIException

import java.lang.Thread.sleep
import scala.jdk.CollectionConverters._

/**
 * @author uchida
 */
class ProjectUserServiceImpl @Inject() (implicit
    val userWrites: UserWrites,
    projectKey: BacklogProjectKey,
    backlog: BacklogAPIClient
) extends ProjectUserService
    with Logging {

  override def allProjectUsers(projectId: Long): Seq[BacklogUser] = {
    try {
      backlog.getProjectUsers(projectId).asScala.toSeq.map(Convert.toBacklog(_))
    } catch {
      case e: BacklogAPIException =>
        logger.error(e.getMessage, e)
        Seq.empty[BacklogUser]
    }
  }

  override def add(userId: Long) = {
    sleep(200)
    backlog.addProjectUser(projectKey.value, userId)
  }

}
