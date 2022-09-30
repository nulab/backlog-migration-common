package com.nulabinc.backlog.migration.common.client

import java.util

import com.nulabinc.backlog.migration.common.client.params._
import com.nulabinc.backlog.migration.common.conf.BacklogConfiguration
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog4j._
import com.nulabinc.backlog4j.api.option.{GetParams, QueryParams}
import com.nulabinc.backlog4j.conf.BacklogConfigure
import com.nulabinc.backlog4j.http.{
  BacklogHttpClient,
  BacklogHttpClientImpl,
  BacklogHttpResponse,
  NameValuePair
}

import scala.jdk.CollectionConverters._
import scala.language.reflectiveCalls

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
    with BacklogAPIClient
    with Logging {

  import scala.util.control.Exception.allCatch

  private val listeners = scala.collection.mutable.ArrayBuffer.empty[RateLimitEventListener]
  private val rateLimitStatusCode    = 429
  private val rateLimitRetryInterval = 60000
  private val rateLimitRetryLimit    = 3

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

  override def importIssue(params: ImportIssueParams): Issue = retryRateLimit() {
    client.importIssue(params)
  }

  override def importUpdateIssue(params: ImportUpdateIssueParams): Issue = retryRateLimit() {
    client.importUpdateIssue(params)
  }

  override def importDeleteAttachment(
      issueIdOrKey: Any,
      attachmentId: Any,
      params: ImportDeleteAttachmentParams
  ): Attachment = retryRateLimit() {
    client.importDeleteAttachment(issueIdOrKey, attachmentId, params)
  }

  override def importWiki(params: ImportWikiParams): Wiki = retryRateLimit() {
    client.importWiki(params)
  }

  override def delete(
      endpoint: String,
      parameters: util.List[NameValuePair]
  ): BacklogHttpResponse = retryRateLimit() {
    super.delete(endpoint, parameters)
  }

  override def get(
      endpoint: String,
      getParams: GetParams,
      queryParams: QueryParams
  ): BacklogHttpResponse = retryRateLimit() {
    super.get(endpoint, getParams, queryParams)
  }

  override def patch(
      endpoint: String,
      parameters: util.List[NameValuePair],
      headers: util.List[NameValuePair]
  ): BacklogHttpResponse = retryRateLimit() {
    super.patch(endpoint, parameters, headers)
  }

  override def post(
      endpoint: String,
      parameters: util.List[NameValuePair],
      headers: util.List[NameValuePair]
  ): BacklogHttpResponse = retryRateLimit() {
    super.post(endpoint, parameters, headers)
  }

  override def postMultiPart(
      endpoint: String,
      parameters: util.Map[String, AnyRef]
  ): BacklogHttpResponse = retryRateLimit() {
    super.postMultiPart(endpoint, parameters)
  }

  override def put(endpoint: String, parameters: util.List[NameValuePair]): BacklogHttpResponse =
    retryRateLimit() {
      super.put(endpoint, parameters)
    }

  override def addRateLimitEventListener(listener: RateLimitEventListener): Unit =
    listeners += listener

  override def removeRateLimitEventListener(listener: RateLimitEventListener): Unit =
    listeners -= listener

  private def retryRateLimit[T]()(f: => T): T = {
    @annotation.tailrec
    def retry0(errors: List[Throwable], f: => T): T = {
      allCatch.either(f) match {
        case Right(r) => r
        case Left(e) =>
          e match {
            case e: BacklogAPIException if e.getStatusCode == rateLimitStatusCode => {
              if (errors.size + 1 >= rateLimitRetryLimit) {
                throw e
              }

              logger.info(e.getMessage, e)

              val event = RateLimitEvent(e)
              listeners.foreach(_.fired(event))

              Thread.sleep(rateLimitRetryInterval)
              retry0(e :: errors, f)
            }
            case _ => throw e
          }
      }
    }
    retry0(Nil, f)
  }
}
