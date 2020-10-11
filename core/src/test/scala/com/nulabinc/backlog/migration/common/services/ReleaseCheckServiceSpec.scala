package com.nulabinc.backlog.migration.common.services

import org.fusesource.jansi.Ansi
import org.scalatest.flatspec.AsyncFlatSpec
import com.nulabinc.backlog.migration.common.dsl.ConsoleDSL
import com.nulabinc.backlog.migration.common.dsl.HttpDSL
import com.nulabinc.backlog.migration.common.dsl.{HttpError, HttpQuery}
import com.nulabinc.backlog.migration.common.services.BacklogReleaseCheckService
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import spray.json.JsonFormat

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
  override def get[A](query: HttpQuery)(implicit format: JsonFormat[A]): Task[Either[HttpError, A]] = {
    val content = query.baseUrl match {
      case "1" =>
        "1.0.0"
      case "2" =>
        """
            1.0.0

          """.stripMargin
    }

    Task {
      Right(content.asInstanceOf[A])
    }
  }

}

case class TestGitHubHttpDSL() extends HttpDSL[Task] {
  import spray.json._

  override def get[A](query: HttpQuery)(implicit format: JsonFormat[A]): Task[Either[HttpError, A]] = {
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
      Right(content.parseJson.convertTo[A])
    }
  }

}
