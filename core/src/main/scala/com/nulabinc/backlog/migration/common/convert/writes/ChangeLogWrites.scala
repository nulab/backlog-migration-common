package com.nulabinc.backlog.migration.common.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.{Convert, Writes}
import com.nulabinc.backlog.migration.common.domain.BacklogChangeLog
import com.nulabinc.backlog.migration.common.utils.{DateUtil, Logging}
import com.nulabinc.backlog4j.ChangeLog

/**
  * @author uchida
  */
private[common] class ChangeLogWrites @Inject() (
    implicit val attachmentInfoWrites: AttachmentInfoWrites,
    implicit val attributeInfoWrites: AttributeInfoWrites
) extends Writes[ChangeLog, BacklogChangeLog]
    with Logging {

  override def writes(changeLog: ChangeLog): BacklogChangeLog = {
    BacklogChangeLog(
      field = changeLog.getField,
      optOriginalValue =
        Option(changeLog.getOriginalValue).map(DateUtil.formatIfNeeded),
      optNewValue = Option(changeLog.getNewValue).map(DateUtil.formatIfNeeded),
      optAttachmentInfo =
        Option(changeLog.getAttachmentInfo).map(Convert.toBacklog(_)),
      optAttributeInfo =
        Option(changeLog.getAttributeInfo).map(Convert.toBacklog(_)),
      optNotificationInfo = Option(changeLog.getNotificationInfo).map(_.getType)
    )
  }

}
