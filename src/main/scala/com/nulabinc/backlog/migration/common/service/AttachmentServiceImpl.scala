package com.nulabinc.backlog.migration.common.service

import java.io.{File, FileInputStream}
import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.Convert
import com.nulabinc.backlog.migration.common.convert.writes.AttachmentWrites
import com.nulabinc.backlog.migration.common.domain.BacklogAttachment
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog4j.BacklogClient
import com.nulabinc.backlog4j.internal.file.AttachmentDataImpl

import scala.collection.JavaConverters._

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

  def allAttachmentsOfIssue(issueId: Long): Either[Throwable, Seq[BacklogAttachment]] = {
    try {
      val attachments = backlog.getIssueAttachments(issueId).asScala
      Right(attachments.map(Convert.toBacklog(_)))
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        Left(e)
    }
  }

}
