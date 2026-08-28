package com.nulabinc.backlog.migration.common.service

import java.io.InputStream

import com.nulabinc.backlog.migration.common.domain.{
  BacklogAttachment,
  BacklogDocument,
  BacklogDocumentComment,
  BacklogDocumentTag,
  BacklogDocumentTree
}

// Counts of what rewriteIssueMentions did to the issueMention nodes in one
// document. total = rewritten + skippedExternalProject + unresolved.
final case class IssueMentionRewriteStats(
    total: Int,
    rewritten: Int,
    skippedExternalProject: Int,
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
  ): BacklogDocument

  // The document body (ProseMirror JSON) can embed an `issueMention` node
  // carrying a snapshot of a referenced issue's source-space key, numeric
  // id, and project key/id
  // (`{"type":"issueMention","attrs":{"id":"PROJ-1","label":"...",
  // "mentionType":"inline","projectKey":"PROJ","projectId":1,"issueId":2}}`;
  // `issueId`/`projectId` are sometimes absent from real data). Mentions of
  // issues in the source project must be rewritten to the key/id assigned
  // when that issue was recreated at the destination, and to the
  // destination project's key/id, otherwise the app can't resolve the
  // mention. Mentions of any other project are left completely untouched,
  // since this tool migrates one project per run and has no mapping data
  // for other projects. When a same-project mention can't be resolved (the
  // issue wasn't found in either map, e.g. it was deleted at the source or
  // failed to migrate), the mention is left as-is and a warning is logged
  // rather than failing the migration.
  //
  // The same snapshot is duplicated as a bracket tag in the document's plain
  // text mirror (`optPlain`), e.g. `[issueMention id="PROJ-1" label="..."
  // mentionType="inline" projectKey="PROJ" projectId="1" issueId="2"]`. Every
  // mention actually rewritten in the JSON has its exact old/new tag text
  // substituted into `optPlain` too (via literal substring replacement, not
  // regex, since labels may contain unescaped `[`/`]`), so the plain-text
  // mirror doesn't keep pointing at the source space after migration. If the
  // expected old tag text can't be found in `optPlain` (e.g. it drifted from
  // the JSON), that mention's plain text is left unchanged and a warning is
  // logged, again without failing the migration.
  def rewriteIssueMentions(
      document: BacklogDocument,
      issueIdMap: Map[Long, Long],
      issueKeyMap: Map[String, String],
      srcProjectId: Long,
      srcProjectKey: String,
      dstProjectId: Long,
      dstProjectKey: String
  ): (BacklogDocument, IssueMentionRewriteStats)

}
