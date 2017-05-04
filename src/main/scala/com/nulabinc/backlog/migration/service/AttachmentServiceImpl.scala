package com.nulabinc.backlog.migration.service

import java.io.{File, FileInputStream}
import javax.inject.Inject

import com.nulabinc.backlog.migration.convert.Convert
import com.nulabinc.backlog.migration.convert.writes.AttachmentWrites
import com.nulabinc.backlog.migration.domain.BacklogAttachment
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.backlog4j.BacklogClient
import com.nulabinc.backlog4j.internal.file.AttachmentDataImpl

/**
  * @author uchida
  */
class AttachmentServiceImpl @Inject()(implicit val attachmentWrites: AttachmentWrites, backlog: BacklogClient)
    extends AttachmentService
    with Logging {

  override def postAttachment(path: String): Either[Throwable, BacklogAttachment] = {
    val file           = new File(path)
    val attachmentData = new AttachmentDataImpl(file.getName, new FileInputStream(file))
    try {
      Right(Convert.toBacklog(backlog.postAttachment(attachmentData)))
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        Left(e)
    }
  }

}
