package com.nulabinc.backlog.migration.common.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.{Convert, Writes}
import com.nulabinc.backlog.migration.common.domain.BacklogGroup
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog4j.Group

import scala.jdk.CollectionConverters._

/**
  * @author uchida
  */
private[common] class GroupWrites @Inject() (implicit
    val userWrites: UserWrites
) extends Writes[Group, BacklogGroup]
    with Logging {

  override def writes(group: Group): BacklogGroup = {
    BacklogGroup(
      group.getName,
      group.getMembers.asScala.toSeq.map(Convert.toBacklog(_))
    )
  }

}
