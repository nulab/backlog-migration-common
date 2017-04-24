package com.nulabinc.backlog.migration.service

import java.io.{File, FileInputStream}
import javax.inject.Inject

import com.nulabinc.backlog.migration.converter.Backlog4jConverters
import com.nulabinc.backlog.migration.domain.BacklogAttachment
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.backlog4j.BacklogClient
import com.nulabinc.backlog4j.internal.file.AttachmentDataImpl

/**
  * @author uchida
  */
class AttachmentServiceImpl @Inject()(backlog: BacklogClient) extends AttachmentService with Logging {

  override def postAttachment(path: String): Either[Throwable, BacklogAttachment] = {
    val file           = new File(path)
    val attachmentData = new AttachmentDataImpl(file.getName, new FileInputStream(file))
    try {
      Right(Backlog4jConverters.Attachment(backlog.postAttachment(attachmentData)))
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        Left(e)
    }
  }

}
