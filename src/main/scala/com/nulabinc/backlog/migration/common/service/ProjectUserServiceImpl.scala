package com.nulabinc.backlog.migration.common.service

import javax.inject.{Inject, Named}

import com.nulabinc.backlog.migration.common.convert.Convert
import com.nulabinc.backlog.migration.common.domain.BacklogUser
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog.migration.common.convert.writes.UserWrites
import com.nulabinc.backlog4j.{BacklogAPIException, BacklogClient}

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class ProjectUserServiceImpl @Inject()(implicit val userWrites: UserWrites, @Named("projectKey") projectKey: String, backlog: BacklogClient)
    extends ProjectUserService
    with Logging {

  override def allProjectUsers(projectId: Long): Seq[BacklogUser] = {
    try {
      backlog.getProjectUsers(projectId).asScala.map(Convert.toBacklog(_))
    } catch {
      case e: BacklogAPIException =>
        logger.error(e.getMessage, e)
        Seq.empty[BacklogUser]
    }
  }

  override def add(userId: Long) =
    backlog.addProjectUser(projectKey, userId)

}
