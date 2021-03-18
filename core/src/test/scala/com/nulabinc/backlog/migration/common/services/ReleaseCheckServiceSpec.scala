package com.nulabinc.backlog.migration.common.services

import com.nulabinc.backlog.migration.common.dsl.{HttpDSL, HttpQuery}
import com.nulabinc.backlog.migration.common.services.BacklogReleaseCheckService
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.scalatest.flatspec.AsyncFlatSpec

class BacklogReleaseCheckServiceSpec extends AsyncFlatSpec {
  implicit val httpDSL = TestBacklogHttpDSL()

  "BacklogReleaseCheckService" should "parse correct version" in {
    BacklogReleaseCheckService.parse[Task](HttpQuery("1")).runToFuture.map { result =>
      assert(result == Right("1.0.0"))
    }
    BacklogReleaseCheckService.parse[Task](HttpQuery("2")).runToFuture.map { result =>
      assert(result == Right("1.0.0"))
    }
  }
}

class GitHubReleaseCheckServiceSpec extends AsyncFlatSpec {
  implicit val httpDSL = TestGitHubHttpDSL()

  "GitHubReleaseCheckService" should "parse correct version" in {
    GitHubReleaseCheckService.parse[Task](HttpQuery("1")).runToFuture.map { result =>
      assert(result == Right("0.14.0b4"))
    }
  }
}

case class TestBacklogHttpDSL() extends HttpDSL[Task] {
  override def get(query: HttpQuery): Task[Response] = {
    val content = query.baseUrl match {
      case "1" =>
        "1.0.0"
      case "2" =>
        """
            1.0.0

          """.stripMargin
    }

    Task {
      Right(content.getBytes())
    }
  }

}

case class TestGitHubHttpDSL() extends HttpDSL[Task] {

  override def get(query: HttpQuery): Task[Response] = {
    val content =
      """
[
  {
    "tag_name": "0.14.0b4",
    "tarball_url": "https://api.github.com/repos/nulab/BacklogMigration-Redmine/tarball/0.14.0b4",
    "zipball_url": "https://api.github.com/repos/nulab/BacklogMigration-Redmine/zipball/0.14.0b4"
  },
  {
    "tag_name": "0.14.0b3",
    "tarball_url": "https://api.github.com/repos/nulab/BacklogMigration-Redmine/tarball/0.14.0b3",
    "zipball_url": "https://api.github.com/repos/nulab/BacklogMigration-Redmine/zipball/0.14.0b3"
  }
]
    """.stripMargin

    Task {
      Right(content.getBytes())
    }
  }

}
