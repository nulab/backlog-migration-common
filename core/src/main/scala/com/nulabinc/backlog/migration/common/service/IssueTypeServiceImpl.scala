package com.nulabinc.backlog.migration.common.service

import java.lang.Thread.sleep
import javax.inject.Inject

import com.nulabinc.backlog.migration.common.client.BacklogAPIClient
import com.nulabinc.backlog.migration.common.convert.Convert
import com.nulabinc.backlog.migration.common.convert.writes.IssueTypeWrites
import com.nulabinc.backlog.migration.common.domain.{BacklogIssueType, BacklogProjectKey}
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog4j.Project
import com.nulabinc.backlog4j.api.option.AddIssueTypeParams

import scala.jdk.CollectionConverters._

/**
 * @author
 *   uchida
 */
class IssueTypeServiceImpl @Inject() (implicit
    val issueTypeWrites: IssueTypeWrites,
    projectKey: BacklogProjectKey,
    backlog: BacklogAPIClient
) extends IssueTypeService
    with Logging {

  override def allIssueTypes(): Seq[BacklogIssueType] =
    backlog.getIssueTypes(projectKey.value).asScala.toSeq.map(Convert.toBacklog(_))

  override def add(issueType: BacklogIssueType): BacklogIssueType = {
    sleep(500)
    val params = new AddIssueTypeParams(
      projectKey.value,
      issueType.name,
      Project.IssueTypeColor.strValueOf(issueType.color)
    )
    Convert.toBacklog(backlog.addIssueType(params))
  }

  override def remove(issueTypeId: Long, defaultIssueTypeId: Long) = {
    backlog.removeIssueType(projectKey.value, issueTypeId, defaultIssueTypeId)
  }

}
