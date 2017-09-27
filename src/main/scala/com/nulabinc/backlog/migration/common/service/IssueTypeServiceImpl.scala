package com.nulabinc.backlog.migration.common.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.Convert
import com.nulabinc.backlog.migration.common.convert.writes.IssueTypeWrites
import com.nulabinc.backlog.migration.common.domain.{BacklogIssueType, BacklogProjectKey}
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog4j.api.option.AddIssueTypeParams
import com.nulabinc.backlog4j.{BacklogClient, Project}

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class IssueTypeServiceImpl @Inject()(implicit val issueTypeWrites: IssueTypeWrites, projectKey: BacklogProjectKey, backlog: BacklogClient)
    extends IssueTypeService
    with Logging {

  override def allIssueTypes(): Seq[BacklogIssueType] =
    backlog.getIssueTypes(projectKey.value).asScala.map(Convert.toBacklog(_))

  override def add(issueType: BacklogIssueType): BacklogIssueType = {
    val params = new AddIssueTypeParams(projectKey.value, issueType.name, Project.IssueTypeColor.strValueOf(issueType.color))
    Convert.toBacklog(backlog.addIssueType(params))
  }

  override def remove(issueTypeId: Long, defaultIssueTypeId: Long) = {
    backlog.removeIssueType(projectKey.value, issueTypeId, defaultIssueTypeId)
  }

}
