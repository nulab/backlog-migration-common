package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog.migration.common.client.params.ImportUpdateIssueParams
import com.nulabinc.backlog.migration.common.domain.BacklogComment

/**
  * @author uchida
  */
trait CommentService {

  def allCommentsOfIssue(issueId: Long): Seq[BacklogComment]

  def update(setUpdateParam: BacklogComment => ImportUpdateIssueParams)(backlogComment: BacklogComment): Either[Throwable, BacklogComment]

  def setUpdateParam(issueId: Long,
                     propertyResolver: PropertyResolver,
                     toRemoteIssueId: Long => Option[Long],
                     postAttachment: String => Option[Long])(backlogComment: BacklogComment): ImportUpdateIssueParams

}
