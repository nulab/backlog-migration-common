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

  def addRateLimitEventListener(listener: RateLimitEventListener): Unit

  def removeRateLimitEventListener(listener: RateLimitEventListener): Unit
}

case class RateLimitEvent(e: BacklogAPIException)

trait RateLimitEventListener {
  def fired(event: RateLimitEvent): Unit
}
