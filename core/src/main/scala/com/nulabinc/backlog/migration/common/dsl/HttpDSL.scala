package com.nulabinc.backlog.migration.common.dsl

import spray.json.JsonFormat
import simulacrum.typeclass

@typeclass
trait HttpDSL[F[_]] {
  type Response[A] = Either[HttpError, A]

  def get[A](query: HttpQuery)(implicit format: JsonFormat[A]): F[Response[A]]
}

sealed trait HttpError
case class RequestError(error: String)  extends HttpError
case class InvalidResponse(msg: String) extends HttpError
case object ServerDown                  extends HttpError

case class HttpQuery(path: String, baseUrl: String)

object HttpQuery {
  def apply(baseUrl: String): HttpQuery =
    new HttpQuery(baseUrl = baseUrl, path = "")
}
