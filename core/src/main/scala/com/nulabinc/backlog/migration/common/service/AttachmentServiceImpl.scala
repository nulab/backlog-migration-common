package com.nulabinc.backlog.migration.common.service

import java.io.{File, FileInputStream}
import java.lang.Thread.sleep

import javax.inject.Inject
import com.nulabinc.backlog.migration.common.client.BacklogAPIClient
import com.nulabinc.backlog.migration.common.convert.Convert
import com.nulabinc.backlog.migration.common.convert.writes.AttachmentWrites
import com.nulabinc.backlog.migration.common.domain.BacklogAttachment
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog4j.internal.file.AttachmentDataImpl

import scala.jdk.CollectionConverters._

/**
 * @author
 *   uchida
 */
class AttachmentServiceImpl @Inject() (implicit
    val attachmentWrites: AttachmentWrites,
    backlog: BacklogAPIClient
) extends AttachmentService
    with Logging {

  override def postAttachment(
      path: String
  ): Either[Throwable, BacklogAttachment] = {
    val file = new File(path)
    val attachmentData =
      new AttachmentDataImpl(file.getName, new FileInputStream(file))
    try {
      sleep(400)
      Right(Convert.toBacklog(backlog.postAttachment(attachmentData)))
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        Left(e)
    }
  }

  def allAttachmentsOfIssue(
      issueId: Long
  ): Either[Throwable, Seq[BacklogAttachment]] = {
    try {
      val attachments = backlog.getIssueAttachments(issueId).asScala.toSeq
      Right(attachments.map(Convert.toBacklog(_)))
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        Left(e)
    }
  }

}
