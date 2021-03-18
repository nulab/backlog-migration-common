package com.nulabinc.backlog.migration.common.service

import cats.Monad
import com.nulabinc.backlog.migration.common.client.params.ImportUpdateIssueParams
import com.nulabinc.backlog.migration.common.domain.BacklogComment
import com.nulabinc.backlog.migration.common.dsl.ConsoleDSL

/**
 * @author uchida
 */
trait CommentService {

  def allCommentsOfIssue(issueId: Long): Seq[BacklogComment]

  def update(setUpdateParam: BacklogComment => ImportUpdateIssueParams)(
      backlogComment: BacklogComment
  ): Either[Throwable, BacklogComment]

  def setUpdateParam[F[_]: Monad: ConsoleDSL](
      issueId: Long,
      propertyResolver: PropertyResolver,
      toRemoteIssueId: Long => Option[Long],
      postAttachment: String => Option[Long]
  )(backlogComment: BacklogComment): ImportUpdateIssueParams

}
