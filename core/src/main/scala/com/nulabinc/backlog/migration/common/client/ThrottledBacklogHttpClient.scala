package com.nulabinc.backlog.migration.common.client

import java.util

import com.nulabinc.backlog4j.api.option.{GetParams, QueryParams}
import com.nulabinc.backlog4j.http.{BacklogHttpClient, BacklogHttpResponse, NameValuePair}

/**
 * Paces every request from the rate-limit headers of the ones before. Sits under
 * `BacklogClientImpl`, so it sees each request once from either client in `BacklogAPIClientImpl`,
 * and sees a 429 before backlog4j turns it into an exception.
 */
class ThrottledBacklogHttpClient(underlying: BacklogHttpClient, limiter: BacklogRateLimiter)
    extends BacklogHttpClient {

  override def setApiKey(apiKey: String): Unit = underlying.setApiKey(apiKey)

  override def setBearerToken(bearerToken: String): Unit = underlying.setBearerToken(bearerToken)

  override def setReadTimeout(readTimeout: Int): Unit = underlying.setReadTimeout(readTimeout)

  override def setConnectionTimeout(connectionTimeout: Int): Unit =
    underlying.setConnectionTimeout(connectionTimeout)

  override def setUserAgent(userAgent: String): Unit = underlying.setUserAgent(userAgent)

  override def getUserAgent: String = underlying.getUserAgent

  override def get(
      endpoint: String,
      getParams: GetParams,
      queryParams: QueryParams
  ): BacklogHttpResponse =
    paced("GET", endpoint)(underlying.get(endpoint, getParams, queryParams))

  override def post(
      endpoint: String,
      parameters: util.List[NameValuePair],
      headers: util.List[NameValuePair]
  ): BacklogHttpResponse =
    paced("POST", endpoint)(underlying.post(endpoint, parameters, headers))

  override def patch(
      endpoint: String,
      parameters: util.List[NameValuePair],
      headers: util.List[NameValuePair]
  ): BacklogHttpResponse =
    paced("PATCH", endpoint)(underlying.patch(endpoint, parameters, headers))

  override def put(endpoint: String, parameters: util.List[NameValuePair]): BacklogHttpResponse =
    paced("PUT", endpoint)(underlying.put(endpoint, parameters))

  override def delete(
      endpoint: String,
      parameters: util.List[NameValuePair]
  ): BacklogHttpResponse =
    paced("DELETE", endpoint)(underlying.delete(endpoint, parameters))

  override def postMultiPart(
      endpoint: String,
      parameters: util.Map[String, AnyRef]
  ): BacklogHttpResponse =
    paced("POST", endpoint)(underlying.postMultiPart(endpoint, parameters))

  private def paced(method: String, endpoint: String)(
      send: => BacklogHttpResponse
  ): BacklogHttpResponse = {
    val bucket = RateLimitBucket.of(method, endpoint)
    limiter.throttle(bucket)
    val response = send
    limiter.record(bucket, response)
    response
  }
}
