package com.nulabinc.backlog.migration.common.client

import java.io.{ByteArrayInputStream, InputStream}
import java.util
import java.util.Date

import com.nulabinc.backlog.migration.common.conf.RequestIntervals
import com.nulabinc.backlog4j.api.option.{GetParams, QueryParams}
import com.nulabinc.backlog4j.http.{BacklogHttpClient, BacklogHttpResponse, NameValuePair}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._

/** Sleeping advances the fake clock, so a test reads what the client waited off `sleeps`. */
class BacklogRateLimiterSpec extends AnyFlatSpec with Matchers {

  private val base  = "https://example.backlog.com/api/v2"
  private val start = 1700000000000L

  class Fixture(
      floors: RequestIntervals = new RequestIntervals(0.millis, 0.millis, adaptive = true)
  ) {
    var now: Long                 = start // the wall clock, compared against reset times
    var tick: Long                = 0L    // the monotonic ticker, used for spacing
    val sleeps: ArrayBuffer[Long] = ArrayBuffer.empty
    val sent: ArrayBuffer[String] = ArrayBuffer.empty

    var answer: BacklogHttpResponse = response(0, 0, None)

    val limiter = new BacklogRateLimiter(
      floors,
      () => now,
      () => tick,
      ms => { sleeps += ms; now += ms; tick += ms }
    )
    val client = new ThrottledBacklogHttpClient(new Underlying, limiter)

    def resetAt(inSeconds: Long): Long = now / 1000 + inSeconds

    /** Time goes by without a request. */
    def pass_(millis: Long): Unit = { now += millis; tick += millis }

    def get(path: String): Unit    = client.get(s"$base/$path", null, null)
    def post(path: String): Unit   = client.post(s"$base/$path", new util.ArrayList(), null)
    def delete(path: String): Unit = client.delete(s"$base/$path", new util.ArrayList())

    def upload(path: String): Unit =
      client.postMultiPart(s"$base/$path", new util.HashMap[String, AnyRef]())

    class Underlying extends BacklogHttpClient {
      override def setApiKey(apiKey: String): Unit                    = ()
      override def setBearerToken(bearerToken: String): Unit          = ()
      override def setReadTimeout(readTimeout: Int): Unit             = ()
      override def setConnectionTimeout(connectionTimeout: Int): Unit = ()
      override def setUserAgent(userAgent: String): Unit              = ()
      override def getUserAgent: String                               = "test"

      override def get(e: String, g: GetParams, q: QueryParams): BacklogHttpResponse =
        serve("GET", e)
      override def post(
          e: String,
          p: util.List[NameValuePair],
          h: util.List[NameValuePair]
      ): BacklogHttpResponse = serve("POST", e)
      override def patch(
          e: String,
          p: util.List[NameValuePair],
          h: util.List[NameValuePair]
      ): BacklogHttpResponse = serve("PATCH", e)
      override def put(e: String, p: util.List[NameValuePair]): BacklogHttpResponse =
        serve("PUT", e)
      override def delete(e: String, p: util.List[NameValuePair]): BacklogHttpResponse =
        serve("DELETE", e)
      override def postMultiPart(e: String, p: util.Map[String, AnyRef]): BacklogHttpResponse =
        serve("POST", e)

      private def serve(method: String, endpoint: String): BacklogHttpResponse = {
        sent += s"$method $endpoint"
        answer
      }
    }
  }

  private def response(
      limit: Int,
      remaining: Int,
      resetAt: Option[Long],
      status: Int = 200
  ): BacklogHttpResponse =
    new BacklogHttpResponse {
      override def getStatusCode: Int          = status
      override def getRateLimitLimit: Int      = limit
      override def getRateLimitRemaining: Int  = remaining
      override def getRateLimitResetDate: Date = resetAt.map(s => new Date(s * 1000)).orNull
      override def getRateLimitReset: String   = resetAt.map(_.toString).orNull
      override def asInputStream: InputStream  = new ByteArrayInputStream(Array.empty)
      override def asString: String            = ""
      override def getFileNameFromContentDisposition: String = null
    }

  "the throttled client" should "send the first request in a bucket straight away" in new Fixture {
    post("issues/import")
    sleeps shouldBe empty
    sent shouldBe Seq(s"POST $base/issues/import")
  }

  it should "pace the next write from the limit the previous write reported" in new Fixture {
    answer = response(150, 149, Some(resetAt(60)))
    post("issues/import")
    post("issues/import")
    sleeps shouldBe Seq(440L)
  }

  it should "pace reads from what reads report, faster than writes" in new Fixture {
    answer = response(600, 599, Some(resetAt(60)))
    get("issues/1")
    get("issues/2")
    sleeps shouldBe Seq(110L)
  }

  it should "not pace a write from what a read reported" in new Fixture {
    answer = response(600, 599, Some(resetAt(60)))
    get("issues/1")
    get("issues/2")
    sleeps shouldBe Seq(110L)
    // Nothing is known about the update bucket yet, and the floor is zero.
    answer = response(0, 0, None)
    post("issues/import")
    sleeps shouldBe Seq(110L)
    limiter.window(RateLimitBucket.Update) shouldBe None
  }

  it should "not pace a read from what a write reported" in new Fixture {
    answer = response(150, 149, Some(resetAt(60)))
    post("issues/import")
    post("issues/import")
    sleeps shouldBe Seq(440L)
    get("issues/1")
    sleeps shouldBe Seq(440L)
  }

  it should "keep searches apart from plain reads" in new Fixture {
    answer = response(150, 149, Some(resetAt(60)))
    get("issues/count")
    limiter.window(RateLimitBucket.Search).map(_.limit) shouldBe Some(150L)
    limiter.window(RateLimitBucket.Read) shouldBe None
    get("issues/1")
    sleeps shouldBe empty
  }

  it should "count the round trip toward the gap" in new Fixture {
    answer = response(150, 149, Some(resetAt(60)))
    post("issues/import")
    pass_(300) // the response took 300 ms to arrive
    post("issues/import")
    sleeps shouldBe Seq(140L)
  }

  it should "wait for the window to reset when the allowance is nearly gone" in new Fixture {
    answer = response(150, 10, Some(resetAt(30)))
    post("issues/import")
    post("issues/import")
    sleeps shouldBe Seq(31000L) // 30 s to the reset and a second of grace
  }

  it should "send again once the window has rolled over" in new Fixture {
    answer = response(150, 10, Some(resetAt(30)))
    post("issues/import")
    answer = response(150, 149, Some(resetAt(90)))
    post("issues/import") // waited for the reset; the response reports a fresh window
    post("issues/import")
    sleeps shouldBe Seq(31000L, 440L)
  }

  it should "never send faster than the configured floor" in new Fixture(
    new RequestIntervals(read = 200.millis, write = 1.second)
  ) {
    answer = response(150, 149, Some(resetAt(60)))
    post("issues/import")
    post("issues/import")
    sleeps shouldBe Seq(1000L)
    answer = response(600, 599, Some(resetAt(60)))
    get("issues/1")
    get("issues/2")
    sleeps shouldBe Seq(1000L, 200L)
  }

  it should "use the floor when the response carried no headers" in new Fixture(
    new RequestIntervals(read = 110.millis, write = 440.millis)
  ) {
    post("issues/import")
    post("issues/import")
    delete("issues/1/attachments/import/2")
    upload("space/attachment")
    sleeps shouldBe Seq(440L, 440L, 440L)
    limiter.window(RateLimitBucket.Update) shouldBe None
  }

  it should "learn the reset time from the refusal itself" in new Fixture {
    answer = response(150, 149, Some(resetAt(60)))
    post("issues/import")
    pass_(1000) // long enough ago that the spacing asks for no wait
    // Someone else spent the key's allowance; Backlog refuses with the window that is left.
    answer = response(150, 0, Some(resetAt(20)), status = 429)
    post("issues/import")
    sleeps shouldBe empty
    limiter.delayAfterTooManyRequests(RateLimitBucket.Update) shouldBe 21000L
  }

  it should "let a retry after the refusal go out as soon as the window resets" in new Fixture {
    answer = response(150, 0, Some(resetAt(20)), status = 429)
    post("issues/import")
    pass_(limiter.delayAfterTooManyRequests(RateLimitBucket.Update)) // what the retry sleeps
    answer = response(150, 149, Some(resetAt(60)))
    post("issues/import")
    sleeps shouldBe empty // the reset has passed, so nothing more to wait for
  }

  it should "fall back to a full minute after a refusal that said nothing" in new Fixture {
    limiter.delayAfterTooManyRequests(RateLimitBucket.Update) shouldBe 60000L
    answer = response(0, 0, None, status = 429)
    post("issues/import")
    limiter.delayAfterTooManyRequests(RateLimitBucket.Update) shouldBe 60000L
  }

  it should "not let an earlier success stand in for a refusal that said nothing" in new Fixture {
    answer = response(150, 149, Some(resetAt(45)))
    post("issues/import")
    answer = response(0, 0, None, status = 429)
    post("issues/import")
    limiter.delayAfterTooManyRequests(RateLimitBucket.Update) shouldBe 60000L
  }

  it should "keep each bucket's refusal apart" in new Fixture {
    answer = response(150, 0, Some(resetAt(20)), status = 429)
    post("issues/import")
    answer = response(600, 599, Some(resetAt(50)))
    get("projects")
    limiter.delayAfterTooManyRequests(RateLimitBucket.Update) shouldBe 21000L
    limiter.delayAfterTooManyRequests(RateLimitBucket.Read) shouldBe 60000L
  }

  "recording" should "ignore a window older than the one already seen" in new Fixture {
    val reset = resetAt(60)
    answer = response(150, 5, Some(reset))
    post("issues/import")
    // A response from before the reset arrives late, with plenty of the old window left.
    answer = response(150, 140, Some(reset - 60))
    post("issues/import")
    limiter.window(RateLimitBucket.Update) shouldBe Some(RateLimitWindow(150, 5, reset))
  }

  it should "keep the lowest remaining count seen within one window" in new Fixture {
    val reset = resetAt(60)
    answer = response(150, 5, Some(reset))
    post("issues/import")
    // An earlier request's response lands after a later one's.
    answer = response(150, 140, Some(reset))
    post("issues/import")
    limiter.window(RateLimitBucket.Update) shouldBe Some(RateLimitWindow(150, 5, reset))
  }

  it should "move on to a newer window" in new Fixture {
    val reset = resetAt(60)
    answer = response(150, 5, Some(reset))
    post("issues/import")
    answer = response(150, 140, Some(reset + 60))
    post("issues/import")
    limiter.window(RateLimitBucket.Update) shouldBe Some(RateLimitWindow(150, 140, reset + 60))
  }

  "spacing" should "come from the ticker, not the wall clock" in new Fixture {
    answer = response(150, 140, Some(resetAt(60)))
    post("issues/import")
    now -= 3600000L // the wall clock is put back an hour; nothing has really elapsed
    post("issues/import")
    sleeps shouldBe Seq(440L)
    now += 2 * 3600000L // and forward an hour; still only the sleep has elapsed
    post("issues/import")
    sleeps shouldBe Seq(440L, 440L)
  }

  "the floors" should "cover reads on one side and everything else on the other" in {
    val limiter = new BacklogRateLimiter(new RequestIntervals(read = 1.millis, write = 2.millis))
    limiter.floorMillis(RateLimitBucket.Read) shouldBe 1L
    limiter.floorMillis(RateLimitBucket.Update) shouldBe 2L
    limiter.floorMillis(RateLimitBucket.Search) shouldBe 2L
    limiter.floorMillis(RateLimitBucket.Icon) shouldBe 2L
  }

  "RateLimitWindow.of" should "read the headers backlog4j parsed" in {
    RateLimitWindow.of(response(150, 142, Some(1605484860L))) shouldBe
    Some(RateLimitWindow(150, 142, 1605484860L))
  }

  it should "be empty when the response carried no headers" in {
    RateLimitWindow.of(response(0, 0, None)) shouldBe None
    RateLimitWindow.of(response(0, 0, Some(1605484860L))) shouldBe None
    RateLimitWindow.of(response(150, 142, None)) shouldBe None
  }
}
