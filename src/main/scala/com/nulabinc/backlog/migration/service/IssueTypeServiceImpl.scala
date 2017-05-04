package com.nulabinc.backlog.migration.service

import javax.inject.{Inject, Named}

import com.nulabinc.backlog.migration.convert.Convert
import com.nulabinc.backlog.migration.convert.writes.IssueTypeWrites
import com.nulabinc.backlog.migration.domain.BacklogIssueType
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.backlog4j.api.option.AddIssueTypeParams
import com.nulabinc.backlog4j.{BacklogClient, Project}

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class IssueTypeServiceImpl @Inject()(implicit val issueTypeWrites: IssueTypeWrites, @Named("projectKey") projectKey: String, backlog: BacklogClient)
    extends IssueTypeService
    with Logging {

  override def allIssueTypes(): Seq[BacklogIssueType] =
    backlog.getIssueTypes(projectKey).asScala.map(Convert.toBacklog(_))

  override def add(issueType: BacklogIssueType): BacklogIssueType = {
    val params = new AddIssueTypeParams(projectKey, issueType.name, Project.IssueTypeColor.strValueOf(issueType.color))
    Convert.toBacklog(backlog.addIssueType(params))
  }

  override def remove(issueTypeId: Long, defaultIssueTypeId: Long) = {
    backlog.removeIssueType(projectKey, issueTypeId, defaultIssueTypeId)
  }

}
