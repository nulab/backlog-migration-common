package com.nulabinc.backlog.migration.common.services

import cats.Monad.ops._
import cats.{Applicative, Monad}
import com.nulabinc.backlog.migration.common.dsl.{ConsoleDSL, HttpDSL, HttpError, HttpQuery}
import com.nulabinc.backlog.migration.common.messages.ConsoleMessages
import com.nulabinc.backlog.migration.common.utils.Logging
import spray.json.DefaultJsonProtocol._

trait ReleaseCheckService {
  def parse[F[_]: Monad: HttpDSL](query: HttpQuery): F[Either[HttpError, String]]

  def check[F[_]: Monad: HttpDSL: ConsoleDSL](path: String, currentVersion: String): F[Unit] = {
    val query = HttpQuery(path)
    for {
      httpResult <- parse(query)
      _ <- httpResult match {
        case Right(latestVersion) if latestVersion != currentVersion =>
          ConsoleDSL[F].warnln(
            ConsoleMessages.notLatestVersion(current = currentVersion, latest = latestVersion)
          )
        case _ =>
          Applicative[F].pure(())
      }
    } yield ()
  }
}

object BacklogReleaseCheckService extends ReleaseCheckService {
  import com.nulabinc.backlog.migration.common.shared.syntax._

  override def parse[F[_]: Monad: HttpDSL](query: HttpQuery): F[Either[HttpError, String]] = {
    val result = for {
      content <- HttpDSL[F].get[String](query).handleError
    } yield content.trim()
    result.value
  }
}

object GitHubReleaseCheckService extends ReleaseCheckService with Logging {
  import com.nulabinc.backlog.migration.common.shared.syntax._

  case class GitHubRelease(tag_name: String)

  implicit val githubReleaseReads = jsonFormat1(GitHubRelease)

  override def parse[F[_]: Monad: HttpDSL](query: HttpQuery): F[Either[HttpError, String]] = {
    val result = for {
      releases <- HttpDSL[F].get[Seq[GitHubRelease]](query).handleError
    } yield {
      releases.headOption.map(_.tag_name).getOrElse {
        logger.warn("Failed to fetch GitHub releases. Empty array")
        ""
      }
    }

    result.value
  }
}
