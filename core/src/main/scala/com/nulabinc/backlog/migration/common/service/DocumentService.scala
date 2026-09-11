package com.nulabinc.backlog.migration.common.service

import java.io.InputStream

import com.nulabinc.backlog.migration.common.domain.{
  BacklogAttachment,
  BacklogDocument,
  BacklogDocumentComment,
  BacklogDocumentTag,
  BacklogDocumentTree
}

// Counts of what rewriteInlineCommentIds did to the inlineComment marks in
// one document. total = rewritten + unresolved.
final case class InlineCommentRewriteStats(
    total: Int,
    rewritten: Int,
    unresolved: Int
)

// Counts of what rewriteIssueMentions did to the issueMention nodes in one
// document. total = rewritten + skippedExternalProject + unresolved.
final case class IssueMentionRewriteStats(
    total: Int,
    rewritten: Int,
    skippedExternalProject: Int,
    unresolved: Int
)

// Counts of what rewriteDocumentMentions did to the documentMention nodes in
// one document. total = rewritten + skippedExternalProject + unresolved.
final case class DocumentMentionRewriteStats(
    total: Int,
    rewritten: Int,
    skippedExternalProject: Int,
    unresolved: Int
)

// Counts of what rewritePeopleMentions did to the peopleMention nodes in one
// document. total = rewritten + unresolved (no project scoping applies).
final case class PeopleMentionRewriteStats(
    total: Int,
    rewritten: Int,
    unresolved: Int
)

// Counts of what rewriteAttachments did to the attachmentBadge/image nodes in
// one document. total = rewritten + unresolved.
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
  ): (BacklogDocument, InlineCommentRewriteStats)

  // The document body (ProseMirror JSON) can embed an `issueMention` node
  // referencing an issue by source-space key/numeric id and project key/id
  // (`{"type":"issueMention","attrs":{"id":"PROJ-1","label":"...",
  // "mentionType":"inline","projectKey":"PROJ","projectId":1,"issueId":2}}`;
  // `issueId`/`projectId` are sometimes absent). Same-project mentions are
  // rewritten via `issueIdMap`/`issueKeyMap`; other projects are left
  // untouched (no mapping data, one project per run); an unresolved
  // same-project mention is left as-is with a warning (fail-soft). The same
  // snapshot is mirrored as a bracket tag in `optPlain` and kept in sync via
  // literal substring replacement (not regex, since labels can contain raw
  // `[`/`]`); a tag that can't be found is left unchanged with a warning.
  def rewriteIssueMentions(
      document: BacklogDocument,
      issueIdMap: Map[Long, Long],
      issueKeyMap: Map[String, String],
      srcProjectId: Long,
      srcProjectKey: String,
      dstProjectId: Long,
      dstProjectKey: String
  ): (BacklogDocument, IssueMentionRewriteStats)

  // Like rewriteIssueMentions, but for `documentMention` nodes (which also
  // carry a `url` snapshot, rewritten only when it matches the expected
  // `.../document/{projectKey}/[e/]{documentId}` shape). The key difference:
  // a mentioned document may not be created yet when this is called, so
  // `documentIdMap` must come from a complete pass over the whole document
  // tree first, not just the documents processed so far.
  def rewriteDocumentMentions(
      document: BacklogDocument,
      documentIdMap: Map[String, String],
      srcProjectKey: String,
      dstProjectId: Long,
      dstProjectKey: String
  ): (BacklogDocument, DocumentMentionRewriteStats)

  // Like rewriteIssueMentions, but for `peopleMention` nodes (a user
  // reference by source-space numeric id). No project scoping applies; a
  // mention resolves via `userMentionMap` (source user id -> (destination
  // user id, destination display name)) or is left untouched. Both `id` and
  // `label` are rewritten, since the label is that person's display name.
  def rewritePeopleMentions(
      document: BacklogDocument,
      userMentionMap: Map[Long, (Long, String)]
  ): (BacklogDocument, PeopleMentionRewriteStats)

  // Rewrites `attachmentBadge` (non-image) and `image` (image, id parsed out
  // of `src`) attachment references. Both always point at this same
  // document, so `projectKey`/`documentId` are always set to the
  // destination; only the attachment id is looked up via `attachmentIdMap`.
  def rewriteAttachments(
      document: BacklogDocument,
      attachmentIdMap: Map[String, String],
      dstProjectKey: String,
      dstDocumentId: String
  ): (BacklogDocument, AttachmentRewriteStats)

  // Combines rewriteIssueMentions, rewriteDocumentMentions, and
  // rewritePeopleMentions into a single parse + tree-walk + serialize pass
  // over the document body, instead of running each independently
  // back-to-back. Behaves identically to calling all three in sequence.
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
