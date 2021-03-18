package com.nulabinc.backlog.migration.common.dsl

import simulacrum.typeclass

@typeclass
trait HttpDSL[F[_]] {
  type Response = Either[HttpError, Array[Byte]]

  def get(query: HttpQuery): F[Response]
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
