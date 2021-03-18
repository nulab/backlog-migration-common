package com.nulabinc.backlog.migration.common.domain

import org.scalatest.flatspec.AnyFlatSpec

class BacklogIssueSpec extends AnyFlatSpec {

  "findIssueIndex" should "parse issue number from issue key" in {
    Map(
      "TEST-2"      -> 2,
      "AAA_BBB-123" -> 123,
      "F-21"        -> 21
    ).map {
      case (issueKey, expect) =>
        assert(BacklogIssue.findIssueIndex(Some(issueKey)) == Some(expect))
    }
  }

  it should "throw an exception if invalid issue key format given" in {
    intercept[scala.MatchError](BacklogIssue.findIssueIndex(Some("AAA")))
  }
}
