package com.nulabinc.backlog.migration.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.convert.Writes
import com.nulabinc.backlog.migration.domain.BacklogAttachment
import com.nulabinc.backlog.migration.utils.{FileUtil, Logging}
import com.nulabinc.backlog4j.Attachment

/**
  * @author uchida
  */
class AttachmentWrites @Inject()() extends Writes[Attachment, BacklogAttachment] with Logging {

  override def writes(attachment: Attachment): BacklogAttachment = {
    BacklogAttachment(
      optId = Some(attachment.getId),
      name = FileUtil.normalize(attachment.getName)
    )
  }

}
