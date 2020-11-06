package com.nulabinc.backlog.migration.common.services

import cats.Monad.ops._
import cats.{Applicative, Monad}
import com.nulabinc.backlog.migration.common.codec.Decoder
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

  private val decoder = new Decoder[Array[Byte], String] {
    override def decode(arr: Array[Byte]): String =
      new String(arr)
  }

  override def parse[F[_]: Monad: HttpDSL](query: HttpQuery): F[Either[HttpError, String]] = {
    val result = for {
      arr <- HttpDSL[F].get(query).handleError
      content = decoder.decode(arr)
    } yield content.trim()
    result.value
  }
}

object GitHubReleaseCheckService extends ReleaseCheckService with Logging {
  import com.nulabinc.backlog.migration.common.shared.syntax._
  import spray.json._

  private case class GitHubRelease(tag_name: String)

  private implicit val githubReleaseReads = jsonFormat1(GitHubRelease)

  private val decoder = new Decoder[Array[Byte], Seq[GitHubRelease]] {
    override def decode(arr: Array[Byte]): Seq[GitHubRelease] = {
      val str = new String(arr)
      str.parseJson.convertTo[Seq[GitHubRelease]]
    }
  }

  override def parse[F[_]: Monad: HttpDSL](query: HttpQuery): F[Either[HttpError, String]] = {
    val result = for {
      arr <- HttpDSL[F].get(query).handleError
      releases = decoder.decode(arr)
    } yield {
      releases.headOption.map(_.tag_name).getOrElse {
        logger.warn("Failed to fetch GitHub releases. Empty array")
        ""
      }
    }

    result.value
  }
}
