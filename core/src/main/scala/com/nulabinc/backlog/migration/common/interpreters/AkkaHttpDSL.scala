package com.nulabinc.backlog.migration.common.interpreters

import java.net.InetSocketAddress

import com.nulabinc.backlog.migration.common.dsl.{HttpDSL, HttpQuery, RequestError, ServerDown}
import monix.eval.Task
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.model.headers.{BasicHttpCredentials, HttpCredentials}
import org.apache.pekko.http.scaladsl.settings.{ClientConnectionSettings, ConnectionPoolSettings}
import org.apache.pekko.http.scaladsl.{ClientTransport, Http}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.control.NonFatal
import scala.util.{Failure, Try}

class AkkaHttpDSL()(implicit
    actorSystem: ActorSystem,
    exc: ExecutionContext
) extends HttpDSL[Task] {
  private val logger = LoggerFactory.getLogger(getClass)
  private val settings = createOptClientTransport()
    .map { transport =>
      ConnectionPoolSettings(actorSystem).withConnectionSettings(
        ClientConnectionSettings(actorSystem).withTransport(transport)
      )
    }
    .getOrElse(ConnectionPoolSettings(actorSystem))

  private val http    = Http()
  private val timeout = 10.seconds
  private val reqHeaders: Seq[HttpHeader] = Seq(
    headers.`User-Agent`("backlog-migration"),
    headers.`Accept-Charset`(HttpCharsets.`UTF-8`)
  )

  def terminate(): Task[Unit] =
    Task.deferFuture(http.shutdownAllConnectionPools())

  override def get(query: HttpQuery): Task[Response] =
    for {
      serverResponse <- doRequest(createRequest(HttpMethods.GET, query))
    } yield serverResponse

  private def createRequest(method: HttpMethod, query: HttpQuery): HttpRequest =
    HttpRequest(
      method = method,
      uri = Uri(query.baseUrl + query.path)
    ).withHeaders(reqHeaders)

  private def doRequest(request: HttpRequest): Task[Response] = {
    logger.info(s"Execute request $request")
    for {
      response <- Task.deferFuture(http.singleRequest(request, settings = settings))
      data <- Task.deferFuture {
        response.entity.toStrict(timeout).map(_.data.toArray)
      }
    } yield {
      val status = response.status.intValue()
      logger.info(s"Received response with status: $status")
      if (response.status.isFailure()) {
        if (status >= 400 && status < 500)
          Left(RequestError(new String(data)))
        else {
          Left(ServerDown)
        }
      } else {
        logger.info(s"Response data is $data")
        Right(data)
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
