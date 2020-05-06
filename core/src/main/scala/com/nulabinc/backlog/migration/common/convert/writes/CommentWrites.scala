package com.nulabinc.backlog.migration.common.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.{Convert, Writes}
import com.nulabinc.backlog.migration.common.domain.BacklogComment
import com.nulabinc.backlog.migration.common.utils.{
  DateUtil,
  Logging,
  StringUtil
}
import com.nulabinc.backlog4j.IssueComment

import scala.jdk.CollectionConverters._

/**
  * @author uchida
  */
private[common] class CommentWrites @Inject() (
    implicit val changeLogWrites: ChangeLogWrites,
    implicit val notificationWrites: NotificationWrites,
    implicit val userWrites: UserWrites
) extends Writes[IssueComment, BacklogComment]
    with Logging {

  override def writes(comment: IssueComment): BacklogComment = {
    BacklogComment(
      eventType = "comment",
      optIssueId = None,
      optContent = Option(comment.getContent).map(StringUtil.toSafeString),
      changeLogs = comment.getChangeLog.asScala.toSeq.map(Convert.toBacklog(_)),
      notifications =
        comment.getNotifications.asScala.toSeq.map(Convert.toBacklog(_)),
      optCreatedUser = Option(comment.getCreatedUser).map(Convert.toBacklog(_)),
      optCreated = Option(comment.getCreated).map(DateUtil.isoFormat)
    )
  }

}
