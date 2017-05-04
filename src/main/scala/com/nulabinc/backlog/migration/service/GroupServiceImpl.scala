package com.nulabinc.backlog.migration.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.convert.Convert
import com.nulabinc.backlog.migration.convert.writes.GroupWrites
import com.nulabinc.backlog.migration.domain.BacklogGroup
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.backlog4j.BacklogClient
import com.nulabinc.backlog4j.api.option.CreateGroupParams

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class GroupServiceImpl @Inject()(implicit val groupWrites: GroupWrites, backlog: BacklogClient) extends GroupService with Logging {

  override def allGroups(): Seq[BacklogGroup] =
    try {
      backlog.getGroups.asScala.map(Convert.toBacklog(_))
    } catch {
      case e: Throwable =>
        logger.warn(e.getMessage, e)
        Seq.empty[BacklogGroup]
    }

  override def create(group: BacklogGroup, propertyResolver: PropertyResolver) = {
    val memberIds = group.members.flatMap(_.optUserId).flatMap(propertyResolver.optResolvedUserId)
    val params    = new CreateGroupParams(group.name)
    params.members(memberIds.asJava)
    backlog.createGroup(params)
  }

}
