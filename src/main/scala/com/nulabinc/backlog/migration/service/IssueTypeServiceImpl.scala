package com.nulabinc.backlog.migration.service

import javax.inject.{Inject, Named}

import com.nulabinc.backlog.migration.convert.Backlog4jConverters
import com.nulabinc.backlog.migration.domain.BacklogIssueType
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.backlog4j.api.option.AddIssueTypeParams
import com.nulabinc.backlog4j.{BacklogClient, Project}

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class IssueTypeServiceImpl @Inject()(@Named("projectKey") projectKey: String, backlog: BacklogClient) extends IssueTypeService with Logging {

  override def allIssueTypes(): Seq[BacklogIssueType] =
    backlog.getIssueTypes(projectKey).asScala.map(Backlog4jConverters.IssueType.apply)

  override def add(issueType: BacklogIssueType): BacklogIssueType = {
    val params = new AddIssueTypeParams(projectKey, issueType.name, Project.IssueTypeColor.strValueOf(issueType.color))
    Backlog4jConverters.IssueType(backlog.addIssueType(params))
  }

  override def remove(issueTypeId: Long, defaultIssueTypeId: Long) = {
    backlog.removeIssueType(projectKey, issueTypeId, defaultIssueTypeId)
  }

}
