package com.nulabinc.backlog.migration.common.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.Writes
import com.nulabinc.backlog.migration.common.domain.BacklogAttachment
import com.nulabinc.backlog.migration.common.utils.{FileUtil, Logging}
import com.nulabinc.backlog4j.Attachment

/**
 * @author
 *   uchida
 */
private[common] class AttachmentWrites @Inject() ()
    extends Writes[Attachment, BacklogAttachment]
    with Logging {

  override def writes(attachment: Attachment): BacklogAttachment = {
    BacklogAttachment(
      optId = Some(attachment.getId),
      name = FileUtil.normalize(attachment.getName)
    )
  }

}
