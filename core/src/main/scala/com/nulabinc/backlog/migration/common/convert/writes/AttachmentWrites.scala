package com.nulabinc.backlog.migration.common.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.{Convert, Writes}
import com.nulabinc.backlog.migration.common.domain.BacklogAttachment
import com.nulabinc.backlog.migration.common.utils.{DateUtil, FileUtil, Logging}
import com.nulabinc.backlog4j.Attachment

/**
 * @author
 *   uchida
 */
private[common] class AttachmentWrites @Inject() (implicit
    val userWrites: UserWrites
) extends Writes[Attachment, BacklogAttachment]
    with Logging {

  override def writes(attachment: Attachment): BacklogAttachment = {
    BacklogAttachment(
      optId = Some(attachment.getId),
      name = FileUtil.clean(attachment.getName),
      optCreatedUser = Option(attachment.getCreatedUser).map(Convert.toBacklog(_)),
      optCreated = Option(attachment.getCreated).map(DateUtil.isoFormat)
    )
  }

}
