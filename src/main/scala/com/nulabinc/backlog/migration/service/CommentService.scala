package com.nulabinc.backlog.migration.service

import com.nulabinc.backlog.migration.domain.BacklogComment
import com.nulabinc.backlog4j.api.option.ImportUpdateIssueParams

import scalax.file.Path

/**
  * @author uchida
  */
trait CommentService {

  def allCommentsOfIssue(issueId: Long): Seq[BacklogComment]

  def update(setUpdateParam: BacklogComment => ImportUpdateIssueParams)(backlogComment: BacklogComment): Either[Throwable, BacklogComment]

  def setUpdateParam(issueId: Long, path: Path, propertyResolver: PropertyResolver, toRemoteIssueId: (Long) => Option[Long])(
      backlogComment: BacklogComment): ImportUpdateIssueParams

}
