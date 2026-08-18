package com.nulabinc.backlog.migration.common.client

import java.net.http.{
  HttpClient => JHttpClient,
  HttpRequest => JHttpRequest,
  HttpResponse => JHttpResponse
}
import java.net.{URI, URLEncoder}
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util
import java.util.Date

import com.nulabinc.backlog.migration.common.client.params._
import com.nulabinc.backlog.migration.common.conf.BacklogConfiguration
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog4j._
import com.nulabinc.backlog4j.api.option.{GetParams, QueryParams}
import com.nulabinc.backlog4j.conf.BacklogConfigure
import com.nulabinc.backlog4j.http.httpclient.HttpClientBacklogHttpClient
import com.nulabinc.backlog4j.http.{BacklogHttpClient, BacklogHttpResponse, NameValuePair}

import scala.jdk.CollectionConverters._
import scala.language.reflectiveCalls

object BacklogAPIClientImpl extends BacklogConfiguration {
  def create: BacklogHttpClient = {
    val client = new HttpClientBacklogHttpClient()
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

private class JsonBacklogHttpResponse(response: JHttpResponse[String])
    extends BacklogHttpResponse {
  override def getStatusCode: Int = response.statusCode()

  override def getRateLimitLimit: Int =
    response.headers().firstValueAsLong("X-RateLimit-Limit").orElse(0L).toInt

  override def getRateLimitRemaining: Int =
    response.headers().firstValueAsLong("X-RateLimit-Remaining").orElse(0L).toInt

  override def getRateLimitResetDate: Date = {
    val reset = response.headers().firstValueAsLong("X-RateLimit-Reset")
    if (reset.isPresent) new Date(reset.getAsLong * 1000) else null
  }

  override def getRateLimitReset: String =
    response.headers().firstValue("X-RateLimit-Reset").orElse(null)

  override def asInputStream(): java.io.InputStream =
    new java.io.ByteArrayInputStream(response.body().getBytes(StandardCharsets.UTF_8))

  override def asString(): String = response.body()

  override def getFileNameFromContentDisposition: String = null
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

  // Document import APIs require a JSON request body (unlike the form-urlencoded
  // params used by importWiki/importIssue), which backlog4j's BacklogHttpClient
  // cannot send. Talk to these endpoints directly instead.
  private val jsonHttpClient: JHttpClient =
    JHttpClient
      .newBuilder()
      .connectTimeout(Duration.ofMillis(configure.getConnectionTimeout))
      .build()

  private def sendJson(method: String, endpoint: String, jsonBody: String): String = {
    val uriSeparator = if (endpoint.contains("?")) "&" else "?"
    val apiKeyParam  = URLEncoder.encode(configure.getApiKey, StandardCharsets.UTF_8.name())
    val request = JHttpRequest
      .newBuilder()
      .uri(URI.create(s"$endpoint$uriSeparator" + s"apiKey=$apiKeyParam"))
      .timeout(Duration.ofMillis(configure.getReadTimeout))
      .header("Content-Type", "application/json; charset=UTF-8")
      .header("iaah", iaah.value)
      .method(method, JHttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
      .build()
    val response =
      jsonHttpClient.send(request, JHttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
    val statusCode = response.statusCode()
    if (statusCode < 200 || statusCode >= 300) {
      val message =
        if (statusCode == rateLimitStatusCode) "The API usage limit has been exceeded."
        else "backlog api request failed."
      throw new BacklogAPIException(message, new JsonBacklogHttpResponse(response))
    }
    response.body()
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

  override def importDocument(jsonBody: String): String = retryRateLimit() {
    sendJson("POST", buildEndpoint("documents/import"), jsonBody)
  }

  override def importUpdateDocumentContent(documentId: String, jsonBody: String): Unit =
    retryRateLimit() {
      sendJson("PATCH", buildEndpoint(s"documents/$documentId/content/import"), jsonBody)
      ()
    }

  override def importDocumentComment(documentId: String, jsonBody: String): String =
    retryRateLimit() {
      sendJson("POST", buildEndpoint(s"documents/$documentId/comments/import"), jsonBody)
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
