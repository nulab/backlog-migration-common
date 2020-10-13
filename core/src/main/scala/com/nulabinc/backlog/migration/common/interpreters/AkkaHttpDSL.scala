package com.nulabinc.backlog.migration.common.interpreters

import java.net.InetAddress
import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.ClientTransport
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.headers.HttpCredentials
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.http.scaladsl.settings.ClientConnectionSettings
import com.nulabinc.backlog.migration.common.dsl.HttpDSL
import com.nulabinc.backlog.migration.common.dsl.HttpQuery
import com.nulabinc.backlog.migration.common.dsl.RequestError
import com.nulabinc.backlog.migration.common.dsl.ServerDown
import monix.eval.Task
import org.slf4j.{Logger, LoggerFactory}
import spray.json._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.reflect.ClassTag
import scala.util.control.NonFatal
import scala.util.Try
import scala.util.Failure

class AkkaHttpDSL()(implicit
    actorSystem: ActorSystem,
    exc: ExecutionContext
) extends HttpDSL[Task] {
  private val logger = LoggerFactory.getLogger(getClass)
  private val settings = createOptClientTransport().map { transport =>
    ConnectionPoolSettings(actorSystem).withConnectionSettings(
      ClientConnectionSettings(actorSystem).withTransport(transport)
    )
  }.getOrElse(ConnectionPoolSettings(actorSystem))

  private val http             = Http()
  private val timeout          = 10.seconds
  private val maxRedirectCount = 20
  private val reqHeaders: Seq[HttpHeader] = Seq(
    headers.`User-Agent`("backlog-migration"),
    headers.`Accept-Charset`(HttpCharsets.`UTF-8`)
  )

  def terminate(): Task[Unit] =
    Task.deferFuture(http.shutdownAllConnectionPools())

  override def get[A](query: HttpQuery)(implicit format: JsonFormat[A]): Task[Response[A]] =
    for {
      serverResponse <- doRequest(createRequest(HttpMethods.GET, query))
      response = serverResponse.map(_.parseJson.convertTo[A](format))
    } yield response

  private def createRequest(method: HttpMethod, query: HttpQuery): HttpRequest =
    HttpRequest(
      method = method,
      uri = Uri(query.baseUrl + query.path)
    ).withHeaders(reqHeaders)

  private def doRequest(request: HttpRequest): Task[Response[String]] = {
    logger.info(s"Execute request $request")
    for {
      response <- Task.deferFuture(http.singleRequest(request, settings = settings))
      data     <- Task.deferFuture(response.entity.toStrict(timeout).map(_.data.utf8String))
      result = {
        val status = response.status.intValue()
        logger.info(s"Received response with status: $status")
        if (response.status.isFailure()) {
          if (status >= 400 && status < 500)
            Left(RequestError(data))
          else {
            Left(ServerDown)
          }
        } else {
          logger.info(s"Response data is $data")
          Right(data)
        }
      }
    } yield result
  }

  private def parseJson[A](response: String, format: JsonFormat[A])(implicit
      classTag: ClassTag[A]
  ): A = {
    try {
      response.parseJson.convertTo[A](format)
    } catch {
      case NonFatal(ex) =>
        logger.error(s"Failed to parse json error: ${ex.getMessage}")
        logger.error(s"Stacktrace:")
        ex.printStackTrace()
        logger.error(s"Got from server $response")
        logger.error(s"Expected to format of $classTag")
        logger.error(s"This is probably a bug, please contact the maintainer of the library")
        throw ex
    }
  }

  private def followRedirect(req: HttpRequest, count: Int = 0): Task[HttpResponse] = {
    logger.info(s"Following redirection $req")
    Task.deferFuture(http.singleRequest(req, settings = settings)).flatMap { resp =>
      resp.status match {
        case StatusCodes.MovedPermanently | StatusCodes.Found | StatusCodes.SeeOther =>
          resp
            .header[headers.Location]
            .map { loc =>
              resp.entity.discardBytes()
              val locUri = loc.uri
              val newUri = locUri
              val newReq = req.withUri(newUri).withHeaders(reqHeaders)
              if (count < maxRedirectCount) followRedirect(newReq, count + 1)
              else Task.deferFuture(http.singleRequest(newReq))
            }
            .getOrElse(throw new RuntimeException(s"location not found on 302 for ${req.uri}"))
        case _ => Task(resp)
      }
    }
  }

  private def createOptClientTransport(): Option[ClientTransport] =
    createProxyTransport(
      optProxyHost = getSystemProperty("https.proxyHost"),
      optProxyPort = getSystemProperty("https.proxyPort"),
      optProxyCredentials = createCredentials(
        optProxyUser = getSystemProperty("https.proxyUser"),
        optProxyPass = getSystemProperty("https.proxyPassword")
      )
    )

  private def createCredentials(
      optProxyUser: Option[String],
      optProxyPass: Option[String]
  ): Option[BasicHttpCredentials] = {
    bothSome(optProxyUser, optProxyPass).map {
      case (proxyUser, proxyPass) =>
        headers.BasicHttpCredentials(proxyUser, proxyPass)
    }
  }

  private def createProxyTransport(
      optProxyHost: Option[String],
      optProxyPort: Option[String],
      optProxyCredentials: Option[HttpCredentials]
  ): Option[ClientTransport] = {
    bothSome(optProxyHost, optProxyPort).flatMap {
      case (proxyHost, proxyPort) =>
        val inetSocketAddress = InetSocketAddress.createUnresolved(proxyHost, proxyPort.toInt)
        Try(
          optProxyCredentials
            .map(ClientTransport.httpsProxy(inetSocketAddress, _))
            .getOrElse(ClientTransport.httpsProxy(inetSocketAddress))
        ).recoverWith {
          case NonFatal(ex) =>
            logger.error(s"Failed to create ClientTransport. Message: ${ex.getMessage()}")
            Failure(ex)
        }.toOption
    }
  }

  private def getSystemProperty(key: String): Option[String] =
    Option(System.getProperty(key))

  private def bothSome[A](opt1: Option[A], opt2: Option[A]): Option[(A, A)] =
    (opt1, opt2) match {
      case (Some(value1), Some(value2)) =>
        Some((value1, value2))
      case _ =>
        None
    }

}
