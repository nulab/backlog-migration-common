package com.nulabinc.backlog.migration.common.client

import com.nulabinc.backlog.migration.common.client.params._
import com.nulabinc.backlog.migration.common.conf.BacklogConfiguration
import com.nulabinc.backlog4j._
import com.nulabinc.backlog4j.conf.BacklogConfigure

class BacklogAPIClientImpl(configure: BacklogConfigure)
  extends BacklogClientImpl(configure)
    with BacklogAPIClient
    with BacklogConfiguration {

  private val client = new BacklogClientImpl(configure) {

    httpClient.setUserAgent(httpClient.getUserAgent + s"-$productName/$productVersion")

    def importIssue(params: ImportIssueParams): Issue =
      factory.importIssue(post(buildEndpoint("issues/import"), params))
    def importUpdateIssue(params: ImportUpdateIssueParams): Issue =
      factory.createIssue(patch(buildEndpoint("issues/" + params.getIssueIdOrKeyString + "/import"), params))
    def importDeleteAttachment(issueIdOrKey: Any, attachmentId: Any, params: ImportDeleteAttachmentParams): Attachment =
      factory.createAttachment(delete(buildEndpoint("issues/" + issueIdOrKey + "/attachments/import/" + attachmentId), params))
    def importWiki(params: ImportWikiParams): Wiki =
      factory.importWiki(post(buildEndpoint("wikis/import"), params))
  }

  override def importIssue(params: ImportIssueParams): Issue =
    client.importIssue(params)

  override def importUpdateIssue(params: ImportUpdateIssueParams): Issue =
    client.importUpdateIssue(params)

  override def importDeleteAttachment(issueIdOrKey: Any, attachmentId: Any, params: ImportDeleteAttachmentParams): Attachment =
    client.importDeleteAttachment(issueIdOrKey, attachmentId, params)

  override def importWiki(params: ImportWikiParams): Wiki =
    client.importWiki(params)
}
