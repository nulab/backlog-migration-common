package com.nulabinc.backlog.migration.common.service

import java.io.InputStream

import com.nulabinc.backlog.migration.common.domain.{
  BacklogAttachment,
  BacklogDocument,
  BacklogDocumentComment,
  BacklogDocumentTag,
  BacklogDocumentTree
}

/**
 * @author
 *   nulab
 */
trait DocumentService {

  def allDocuments(projectId: Long, offset: Int, count: Int): Seq[BacklogDocument]

  def countDocuments(projectId: Long): Int

  def documentOfId(documentId: String): BacklogDocument

  def documentTree(projectId: Long): BacklogDocumentTree

  def downloadDocumentAttachment(
      documentId: String,
      attachmentId: Long
  ): Option[(String, InputStream)]

  def create(
      projectId: Long,
      document: BacklogDocument,
      optParentId: Option[String],
      addLast: Boolean,
      propertyResolver: PropertyResolver
  ): String

  def updateContent(
      documentId: String,
      document: BacklogDocument,
      propertyResolver: PropertyResolver
  ): Unit

  def addComment(
      documentId: String,
      comment: BacklogDocumentComment,
      propertyResolver: PropertyResolver
  ): Either[Throwable, String]

  def addAttachment(
      documentId: String,
      path: String
  ): Either[Throwable, BacklogAttachment]

  def addTags(
      documentId: String,
      tagNames: Seq[String]
  ): Either[Throwable, Seq[BacklogDocumentTag]]

}
