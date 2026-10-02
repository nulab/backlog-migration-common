package com.nulabinc.backlog.migration.common.client

import com.nulabinc.backlog.migration.common.client.params._
import com.nulabinc.backlog4j.{Attachment, BacklogAPIException, BacklogClient, Issue, Wiki}

trait BacklogAPIClient extends BacklogClient {

  def importIssue(params: ImportIssueParams): Issue

  def importUpdateIssue(params: ImportUpdateIssueParams): Issue

  def importDeleteAttachment(
      issueIdOrKey: Any,
      attachmentId: Any,
      params: ImportDeleteAttachmentParams
  ): Attachment

  def importWiki(params: ImportWikiParams): Wiki

  def importDocument(jsonBody: String): String

  def importUpdateDocumentContent(documentId: String, jsonBody: String): Unit

  def importDocumentComment(documentId: String, jsonBody: String): String

  def importDocumentAttachment(
      documentId: String,
      filename: String,
      content: Array[Byte],
      created: Option[String],
      createdUserId: Option[Long]
  ): String

  def addRateLimitEventListener(listener: RateLimitEventListener): Unit

  def removeRateLimitEventListener(listener: RateLimitEventListener): Unit
}

case class RateLimitEvent(e: BacklogAPIException)

trait RateLimitEventListener {
  def fired(event: RateLimitEvent): Unit
}
