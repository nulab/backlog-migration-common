package com.nulabinc.backlog.migration.service

import com.nulabinc.backlog.migration.domain.{BacklogAttachment, BacklogComment}
import com.nulabinc.backlog4j.api.option.ImportUpdateIssueParams

/**
  * @author uchida
  */
trait CommentService {

  def allCommentsOfIssue(issueId: Long): Seq[BacklogComment]

  def update(setUpdateParam: BacklogComment => ImportUpdateIssueParams)(backlogComment: BacklogComment): Either[Throwable, BacklogComment]

  def setUpdateParam(issueId: Long,
                     propertyResolver: PropertyResolver,
                     toRemoteIssueId: (Long) => Option[Long],
                     postAttachment: (String) => Option[Long])(backlogComment: BacklogComment): ImportUpdateIssueParams

  def postAttachment(path: String): Either[Throwable, BacklogAttachment]

}
