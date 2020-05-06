package com.nulabinc.backlog.migration.common.domain.exports

import com.nulabinc.backlog.migration.common.domain._

case class ExportedBacklogIssue(
    id: Long,
    optIssueKey: Option[String],
    summary: BacklogIssueSummary,
    optParentIssueId: Option[Long],
    description: String,
    optStartDate: Option[String],
    optDueDate: Option[String],
    optEstimatedHours: Option[Float],
    optActualHours: Option[Float],
    optIssueTypeName: Option[String],
    status: ExportedBacklogStatus,
    categoryNames: Seq[String],
    versionNames: Seq[String],
    milestoneNames: Seq[String],
    priorityName: String,
    optAssignee: Option[BacklogUser],
    attachments: Seq[BacklogAttachment],
    sharedFiles: Seq[BacklogSharedFile],
    customFields: Seq[BacklogCustomField],
    notifiedUsers: Seq[BacklogUser],
    operation: BacklogOperation
)
