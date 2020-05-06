package com.nulabinc.backlog.migration.common.client

import com.nulabinc.backlog.migration.common.client.params._
import com.nulabinc.backlog4j.{Attachment, BacklogClient, Issue, Wiki}

trait BacklogAPIClient extends BacklogClient {

  def importIssue(params: ImportIssueParams): Issue

  def importUpdateIssue(params: ImportUpdateIssueParams): Issue

  def importDeleteAttachment(
      issueIdOrKey: Any,
      attachmentId: Any,
      params: ImportDeleteAttachmentParams
  ): Attachment

  def importWiki(params: ImportWikiParams): Wiki
}
