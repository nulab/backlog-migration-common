package com.nulabinc.backlog.migration.common.service

import java.io.InputStream
import java.lang.Thread.sleep
import javax.inject.Inject

import com.nulabinc.backlog.migration.common.client.BacklogAPIClient
import com.nulabinc.backlog.migration.common.convert.Convert
import com.nulabinc.backlog.migration.common.convert.writes.{DocumentCommentWrites, DocumentWrites}
import com.nulabinc.backlog.migration.common.domain.BacklogDocument
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog4j.api.option.{GetDocumentsCountParams, GetDocumentsParams}

import scala.jdk.CollectionConverters._

/**
 * @author
 *   nulab
 */
class DocumentServiceImpl @Inject() (implicit
    val documentWrites: DocumentWrites,
    val documentCommentWrites: DocumentCommentWrites,
    backlog: BacklogAPIClient
) extends DocumentService
    with Logging {

  override def allDocuments(
      projectId: Long,
      offset: Int,
      count: Int
  ): Seq[BacklogDocument] = {
    val params = new GetDocumentsParams(List[java.lang.Long](projectId).asJava, offset.toLong)
    params.count(count)
    params.sort(GetDocumentsParams.SortKey.Created)
    params.order(GetDocumentsParams.Order.Asc)
    try {
      backlog
        .getDocuments(params)
        .asScala
        .toSeq
        .map(document => withComments(Convert.toBacklog(document)))
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        Seq.empty[BacklogDocument]
    }
  }

  override def countDocuments(projectId: Long): Int = {
    val params = new GetDocumentsCountParams(projectId)
    try {
      backlog.getDocumentsCount(params)
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        0
    }
  }

  override def documentOfId(documentId: String): BacklogDocument = {
    sleep(500)
    withComments(Convert.toBacklog(backlog.getDocument(documentId)))
  }

  override def downloadDocumentAttachment(
      documentId: String,
      attachmentId: Long
  ): Option[(String, InputStream)] =
    try {
      val attachmentData = backlog.downloadDocumentAttachment(documentId, attachmentId)
      if (attachmentData.getFilename.isEmpty) {
        None
      } else {
        Some((attachmentData.getFilename, attachmentData.getContent))
      }
    } catch {
      case e: Throwable =>
        logger.warn(e.getMessage, e)
        None
    }

  private[this] def withComments(document: BacklogDocument): BacklogDocument = {
    val comments =
      try {
        backlog.getDocumentComments(document.id).asScala.toSeq.map(Convert.toBacklog(_))
      } catch {
        case e: Throwable =>
          logger.warn(e.getMessage, e)
          Seq.empty
      }
    document.copy(comments = comments)
  }

}
