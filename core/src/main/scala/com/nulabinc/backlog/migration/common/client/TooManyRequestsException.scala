package com.nulabinc.backlog.migration.common.client

import java.text.SimpleDateFormat

import com.nulabinc.backlog4j.BacklogAPIException
import com.nulabinc.backlog4j.http.BacklogHttpResponse

/**
 * A 429 as `ThrottledBacklogHttpClient` saw it. The window the refusal reported travels with the
 * exception, so the retry for this request waits on this refusal's reset and not on whichever
 * refusal in the same bucket happened to be recorded last.
 */
class TooManyRequestsException(response: BacklogHttpResponse)
    extends BacklogAPIException(TooManyRequestsException.message(response), response) {

  /** `None` when the refusal carried no rate-limit headers. */
  val window: Option[RateLimitWindow] = RateLimitWindow.of(response)
}

object TooManyRequestsException {

  /** Word for word what backlog4j's own 429 says, so logs and listeners see no difference. */
  private def message(response: BacklogHttpResponse): String = {
    val resetAt = Option(response.getRateLimitResetDate)
      .map(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(_))
      .getOrElse("")
    s"The API usage limit has been exceeded, and will be available again from $resetAt."
  }
}
