package com.nulabinc.backlog.migration.common.client

import com.nulabinc.backlog.migration.common.client.params._
import com.nulabinc.backlog.migration.common.conf.BacklogConfiguration
import com.nulabinc.backlog4j._
import com.nulabinc.backlog4j.conf.BacklogConfigure
import com.nulabinc.backlog4j.http.{BacklogHttpClient, BacklogHttpClientImpl, NameValuePair}

import scala.jdk.CollectionConverters._

object BacklogAPIClientImpl extends BacklogConfiguration {
  def create: BacklogHttpClient = {
    val client = new BacklogHttpClientImpl()
    client.setUserAgent(
      s"backlog4j/${backlog4jVersion}-$productName/$productVersion"
    )
    client
  }
}

case class IAAH(value: String) extends AnyVal

object IAAH {
  val empty: IAAH = IAAH("")
}

class BacklogAPIClientImpl(configure: BacklogConfigure, iaah: IAAH)
    extends BacklogClientImpl(configure, BacklogAPIClientImpl.create)
    with BacklogAPIClient {

  private val client =
    new BacklogClientImpl(configure, BacklogAPIClientImpl.create) {
      val headers = Seq(
        new NameValuePair("iaah", iaah.value)
      ).asJava

      def importIssue(params: ImportIssueParams): Issue =
        factory.importIssue(post(buildEndpoint("issues/import"), params.getParamList, headers))

      def importUpdateIssue(params: ImportUpdateIssueParams): Issue =
        factory.createIssue(
          patch(
            buildEndpoint("issues/" + params.getIssueIdOrKeyString + "/import"),
            params.getParamList,
            headers
          )
        )
      def importDeleteAttachment(
          issueIdOrKey: Any,
          attachmentId: Any,
          params: ImportDeleteAttachmentParams
      ): Attachment =
        factory.createAttachment(
          delete(
            buildEndpoint(
              "issues/" + issueIdOrKey + "/attachments/import/" + attachmentId
            ),
            params
          )
        )
      def importWiki(params: ImportWikiParams): Wiki =
        factory.importWiki(post(buildEndpoint("wikis/import"), params.getParamList, headers))
    }

  override def importIssue(params: ImportIssueParams): Issue =
    client.importIssue(params)

  override def importUpdateIssue(params: ImportUpdateIssueParams): Issue =
    client.importUpdateIssue(params)

  override def importDeleteAttachment(
      issueIdOrKey: Any,
      attachmentId: Any,
      params: ImportDeleteAttachmentParams
  ): Attachment =
    client.importDeleteAttachment(issueIdOrKey, attachmentId, params)

  override def importWiki(params: ImportWikiParams): Wiki =
    client.importWiki(params)
}
