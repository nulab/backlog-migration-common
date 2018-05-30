package com.nulabinc.backlog.migration.common.client

import com.nulabinc.backlog4j.{Attachment, BacklogClient, Issue, Wiki}
import com.nulabinc.backlog4j.api.option.ImportDeleteAttachmentParams
import com.nulabinc.backlog4j.api.option.ImportIssueParams
import com.nulabinc.backlog4j.api.option.ImportUpdateIssueParams
import com.nulabinc.backlog4j.api.option.ImportWikiParams

trait BacklogAPIClient extends BacklogClient {

  def importIssue(params: ImportIssueParams): Issue

  def importUpdateIssue(params: ImportUpdateIssueParams): Issue

  def importDeleteAttachment(issueIdOrKey: Any, attachmentId: Any, params: ImportDeleteAttachmentParams): Attachment

  def importWiki(params: ImportWikiParams): Wiki
}
