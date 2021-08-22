package com.nulabinc.backlog.migration.common.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.{Convert, Writes}
import com.nulabinc.backlog.migration.common.domain._
import com.nulabinc.backlog.migration.common.utils.{DateUtil, Logging, StringUtil}
import com.nulabinc.backlog4j.Issue

import scala.jdk.CollectionConverters._

/**
 * @author
 *   uchida
 */
private[common] class IssueWrites @Inject() (
    implicit val userWrites: UserWrites,
    implicit val sharedFileWrites: SharedFileWrites,
    implicit val customFieldWrites: CustomFieldWrites
) extends Writes[Issue, BacklogIssue]
    with Logging {

  override def writes(issue: Issue): BacklogIssue = {
    BacklogIssue(
      eventType = "issue",
      id = issue.getId,
      issueKey = issue.getIssueKey,
      summary = BacklogIssueSummary(
        value = StringUtil.toSafeString(issue.getSummary),
        original = StringUtil.toSafeString(issue.getSummary)
      ),
      optParentIssueId = parentIssueId(issue),
      description = StringUtil.toSafeString(issue.getDescription),
      optStartDate = Option(issue.getStartDate).map(DateUtil.dateFormat),
      optDueDate = Option(issue.getDueDate).map(DateUtil.dateFormat),
      optEstimatedHours = Option(issue.getEstimatedHours).map(_.floatValue()),
      optActualHours = Option(issue.getActualHours).map(_.floatValue()),
      optIssueTypeName = Some(issue.getIssueType.getName),
      status = BacklogStatus.from(issue.getStatus),
      categoryNames = issue.getCategory.asScala.toSeq.map(_.getName),
      versionNames = issue.getVersions.asScala.toSeq.map(_.getName),
      milestoneNames = issue.getMilestone.asScala.toSeq.map(_.getName),
      priorityName = Option(issue.getPriority).map(_.getName).getOrElse(""),
      optAssignee = Option(issue.getAssignee).map(Convert.toBacklog(_)),
      attachments = Seq.empty[BacklogAttachment],
      sharedFiles = issue.getSharedFiles.asScala.toSeq.map(Convert.toBacklog(_)),
      customFields = issue.getCustomFields.asScala.toSeq.flatMap(Convert.toBacklog(_)),
      notifiedUsers = Seq.empty[BacklogUser],
      operation = toBacklogOperation(issue)
    )
  }

  private[this] def parentIssueId(issue: Issue): Option[Long] = {
    Option(issue.getParentIssueId) match {
      case Some(id) if id == 0 => None
      case Some(id)            => Some(id)
      case _                   => None
    }
  }

  private[this] def toBacklogOperation(issue: Issue): BacklogOperation =
    BacklogOperation(
      optCreatedUser = Option(issue.getCreatedUser).map(Convert.toBacklog(_)),
      optCreated = Option(issue.getCreated).map(DateUtil.isoFormat),
      optUpdatedUser = Option(issue.getUpdatedUser).map(Convert.toBacklog(_)),
      optUpdated = Option(issue.getUpdated).map(DateUtil.isoFormat)
    )

}
