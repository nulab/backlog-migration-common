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

  // The document body (ProseMirror JSON) anchors each inline comment to a
  // range of text via an `inlineComment` mark carrying the comment's id
  // (`{"type":"inlineComment","attrs":{"comment":{"id":"...","statusId":...}}}`).
  // That id is only valid within the source space, so it must be rewritten to
  // the id assigned when the comment was recreated at the destination,
  // otherwise the app can't resolve the mark and the comment isn't shown as
  // linked to the document.
  def rewriteInlineCommentIds(
      document: BacklogDocument,
      commentIdMap: Map[String, String]
  ): BacklogDocument

}
