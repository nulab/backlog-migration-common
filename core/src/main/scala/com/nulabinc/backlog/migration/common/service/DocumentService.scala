package com.nulabinc.backlog.migration.common.service

import java.io.InputStream

import com.nulabinc.backlog.migration.common.domain.BacklogDocument

/**
 * @author
 *   nulab
 */
trait DocumentService {

  def allDocuments(projectId: Long, offset: Int, count: Int): Seq[BacklogDocument]

  def countDocuments(projectId: Long): Int

  def documentOfId(documentId: String): BacklogDocument

  def downloadDocumentAttachment(
      documentId: String,
      attachmentId: Long
  ): Option[(String, InputStream)]

}
