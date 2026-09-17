package com.nulabinc.backlog.migration.common.client

import com.nulabinc.backlog.migration.common.client.RateLimitBucket._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RateLimitBucketSpec extends AnyFlatSpec with Matchers {

  private val base = "https://example.backlog.com/api/v2"

  "a GET" should "count as a read" in {
    RateLimitBucket.of("GET", s"$base/issues/1") shouldBe Read
    RateLimitBucket.of("GET", s"$base/projects/KEY/statuses") shouldBe Read
    RateLimitBucket.of("GET", s"$base/users") shouldBe Read
    RateLimitBucket.of("GET", s"$base/space") shouldBe Read
  }

  it should "count as a search when it lists or counts issues or wikis" in {
    RateLimitBucket.of("GET", s"$base/issues") shouldBe Search
    RateLimitBucket.of("GET", s"$base/issues/count") shouldBe Search
    RateLimitBucket.of("GET", s"$base/wikis") shouldBe Search
    RateLimitBucket.of("GET", s"$base/wikis/count") shouldBe Search
  }

  it should "count as an icon fetch for the image endpoints" in {
    RateLimitBucket.of("GET", s"$base/space/image") shouldBe Icon
    RateLimitBucket.of("GET", s"$base/users/12/icon") shouldBe Icon
    RateLimitBucket.of("GET", s"$base/projects/KEY/image") shouldBe Icon
    RateLimitBucket.of("GET", s"$base/groups/3/icon") shouldBe Icon
    RateLimitBucket.of("GET", s"$base/teams/3/icon") shouldBe Icon
  }

  it should "not mistake an endpoint that merely starts like a search or icon one" in {
    RateLimitBucket.of("GET", s"$base/issues/1/comments") shouldBe Read
    RateLimitBucket.of("GET", s"$base/issues/1/comments/count") shouldBe Read
    RateLimitBucket.of("GET", s"$base/wikis/5") shouldBe Read
    RateLimitBucket.of("GET", s"$base/users/12/icon/extra") shouldBe Read
  }

  it should "ignore a query string, a trailing slash and the case of the method" in {
    RateLimitBucket.of("get", s"$base/issues?projectId[]=1&count=100") shouldBe Search
    RateLimitBucket.of("GET", s"$base/issues/") shouldBe Search
    RateLimitBucket.of("GET", s"$base/issues/count?projectId[]=1") shouldBe Search
  }

  it should "be classified from a bare path as well as a full URL" in {
    RateLimitBucket.of("GET", "/api/v2/issues") shouldBe Search
    RateLimitBucket.of("GET", "issues") shouldBe Search
    RateLimitBucket.of("GET", "https://example.backlog.jp/api/v2/space/image") shouldBe Icon
  }

  "anything but a GET" should "count as an update" in {
    RateLimitBucket.of("POST", s"$base/issues/import") shouldBe Update
    RateLimitBucket.of("PATCH", s"$base/issues/1/import") shouldBe Update
    RateLimitBucket.of("PUT", s"$base/issues/1") shouldBe Update
    RateLimitBucket.of("DELETE", s"$base/issues/1/attachments/import/2") shouldBe Update
    RateLimitBucket.of("POST", s"$base/issues") shouldBe Update // not a search
    RateLimitBucket.of("POST", s"$base/space/attachment") shouldBe Update
  }
}
