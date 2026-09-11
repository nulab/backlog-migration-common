package com.nulabinc.backlog.migration.common.service

import java.io.InputStream

import com.nulabinc.backlog.migration.common.domain.{
  BacklogAttachment,
  BacklogDocument,
  BacklogDocumentComment,
  BacklogDocumentTag,
  BacklogDocumentTree
}

// total = rewritten + unresolved
final case class InlineCommentRewriteStats(
    total: Int,
    rewritten: Int,
    unresolved: Int
)

// total = rewritten + skippedExternalProject + unresolved
final case class IssueMentionRewriteStats(
    total: Int,
    rewritten: Int,
    skippedExternalProject: Int,
    unresolved: Int
)

// total = rewritten + skippedExternalProject + unresolved
final case class DocumentMentionRewriteStats(
    total: Int,
    rewritten: Int,
    skippedExternalProject: Int,
    unresolved: Int
)

// total = rewritten + unresolved
final case class PeopleMentionRewriteStats(
    total: Int,
    rewritten: Int,
    unresolved: Int
)

// total = rewritten + unresolved
final case class AttachmentRewriteStats(
    total: Int,
    rewritten: Int,
    unresolved: Int
)

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
      isTrash: Boolean,
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

  // Rewrites the comment id carried by each `inlineComment` mark in the
  // document body from its source-space id to the id assigned when the
  // comment was recreated at the destination.
  def rewriteInlineCommentIds(
      document: BacklogDocument,
      commentIdMap: Map[String, String]
  ): (BacklogDocument, InlineCommentRewriteStats)

  // Rewrites `issueMention` nodes referencing issues in the same project
  // (via `issueIdMap`/`issueKeyMap`); mentions of other projects are left
  // untouched, and an unresolved same-project mention is left as-is with a
  // warning. The mirrored bracket tag in `optPlain` is kept in sync too.
  def rewriteIssueMentions(
      document: BacklogDocument,
      issueIdMap: Map[Long, Long],
      issueKeyMap: Map[String, String],
      srcProjectId: Long,
      srcProjectKey: String,
      dstProjectId: Long,
      dstProjectKey: String
  ): (BacklogDocument, IssueMentionRewriteStats)

  // Like rewriteIssueMentions, but for `documentMention` nodes. `documentIdMap`
  // must come from a complete pass over the whole document tree first, since
  // a mentioned document may not be created yet when this is called.
  def rewriteDocumentMentions(
      document: BacklogDocument,
      documentIdMap: Map[String, String],
      srcProjectKey: String,
      dstProjectId: Long,
      dstProjectKey: String
  ): (BacklogDocument, DocumentMentionRewriteStats)

  // Like rewriteIssueMentions, but for `peopleMention` nodes. No project
  // scoping applies; both `id` and `label` are rewritten via `userMentionMap`.
  def rewritePeopleMentions(
      document: BacklogDocument,
      userMentionMap: Map[Long, (Long, String)]
  ): (BacklogDocument, PeopleMentionRewriteStats)

  // Rewrites `attachmentBadge`/`image` attachment references to point at the
  // destination document, looking up the attachment id via `attachmentIdMap`.
  def rewriteAttachments(
      document: BacklogDocument,
      attachmentIdMap: Map[String, String],
      dstProjectKey: String,
      dstDocumentId: String
  ): (BacklogDocument, AttachmentRewriteStats)

  // Combines rewriteIssueMentions, rewriteDocumentMentions, and
  // rewritePeopleMentions into a single pass over the document body.
  // Behaves identically to calling all three in sequence.
  def rewriteMentions(
      document: BacklogDocument,
      issueIdMap: Map[Long, Long],
      issueKeyMap: Map[String, String],
      documentIdMap: Map[String, String],
      userMentionMap: Map[Long, (Long, String)],
      srcProjectId: Long,
      srcProjectKey: String,
      dstProjectId: Long,
      dstProjectKey: String
  ): (
      BacklogDocument,
      IssueMentionRewriteStats,
      DocumentMentionRewriteStats,
      PeopleMentionRewriteStats
  )

}
