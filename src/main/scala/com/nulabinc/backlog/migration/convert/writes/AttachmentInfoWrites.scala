package com.nulabinc.backlog.migration.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.convert.Writes
import com.nulabinc.backlog.migration.domain.BacklogAttachment
import com.nulabinc.backlog.migration.utils.{FileUtil, Logging}
import com.nulabinc.backlog4j.AttachmentInfo

/**
  * @author uchida
  */
class AttachmentInfoWrites @Inject()() extends Writes[AttachmentInfo, BacklogAttachment] with Logging {

  override def writes(attachmentInfo: AttachmentInfo): BacklogAttachment = {
    BacklogAttachment(optId = Option(attachmentInfo).map(_.getId), name = FileUtil.normalize(attachmentInfo.getName))
  }

}
